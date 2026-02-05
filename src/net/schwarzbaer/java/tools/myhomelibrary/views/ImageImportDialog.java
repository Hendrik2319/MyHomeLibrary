package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.ImageTools;
import net.schwarzbaer.java.lib.gui.ImageView;
import net.schwarzbaer.java.lib.gui.ImageViewDialog;
import net.schwarzbaer.java.lib.gui.ProgressDialog;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.ZoomableCanvas;
import net.schwarzbaer.java.lib.system.DateTimeFormatter;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;

public class ImageImportDialog extends StandardDialog
{
	private static final long serialVersionUID = -6506180541375218481L;
	private static final boolean DEBUG_TEST_ENGINE = false;
	private static final boolean DEBUG_COMPUTE_DUMMY_IMAGE = false;
	private static final boolean DEBUG_OTHER_OUTPUTS = false;
	
	private final BufferedImage originalImage;
	private final ImportView importView;
	
	private BufferedImage result;
	private ImageEditor currentEditor;

	ImageImportDialog(Window window, BufferedImage originalImage, Scenario.Type scenarioType, int width, int height)
	{
		super(window, "Image Import");
		this.originalImage = originalImage;
		result = null;
		importView = new ImportView();
		
		Scenario scenario = null;
		if (scenarioType!=null)
			switch (scenarioType)
			{
				case FullImageManipulation: scenario = new FullImageManipulation(); break;
				case ImageCutOut          : scenario = new ImageCutOut(); break;
			};
		if (scenario == null)
			throw new IllegalArgumentException();
		
		JPanel toolbar = scenario.createDialogBottomToolbar();
		toolbar.setBorder(BorderFactory.createEmptyBorder(0,5,5,5));
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(importView, BorderLayout.CENTER);
		contentPane.add(toolbar, BorderLayout.SOUTH);
		
		createGUI(contentPane, new Dimension(width, height));
		
		setEditor(scenario.getInitialEditor());
	}

	private void setEditor(ImageEditor editor)
	{
		this.currentEditor = editor;
		currentEditor.activate();
		updateWindowTitle();
		importView.setEditor(currentEditor);
	}

	private void updateWindowTitle()
	{
		if (currentEditor==null)
			setTitle("Image Import");
		else
		{
			String extraTitleStr = currentEditor.getExtraTitleStr();
			if (extraTitleStr==null)
				setTitle("Image Import - %s".formatted(currentEditor.title));
			else
				setTitle("Image Import - %s: %s".formatted(currentEditor.title, extraTitleStr));
		}
	}

	public static void test(Window window)
	{
		BufferedImage image = FileIO.loadImagefile("import.test.jpg");
		new ImageViewDialog(window, image, "Loaded Image", 800, 500, true).setVisible(true);
		
		BufferedImage result = showDialog(window, image);
		
		new ImageViewDialog(window, result, "Result Image", 500, 800, true).setVisible(true);
		System.exit(0);
	}

	public static BufferedImage showDialog(Window window, BufferedImage image)
	{
		return showDialog(window, image, Scenario.Type.FullImageManipulation, 800,800);
	}

	private static BufferedImage showDialog(Window window, BufferedImage image, Scenario.Type scenario, int width, int height)
	{
		ImageImportDialog dlg = new ImageImportDialog(window, image, scenario, width, height);
		dlg.showDialog();
		return dlg.result;
	}

	private static class CutOutEngine
	{
		private int SUBRASTER_SIZE = 5;
		private final Window window;
		private final BufferedImage image;
		private final Point topLeft;
		private final Point topRight;
		private final Point bottomLeft;
		private final Point bottomRight;
		private int targetHeight;
		private int targetWidth;
		private GeometryComputations debug_gc;
		
		CutOutEngine(Window window, BufferedImage image)
		{
			this.window = window;
			this.image = image;
			debug_gc = null;
			int imageWidth  = image.getWidth ();
			int imageHeight = image.getHeight();
			
			topLeft     = new Point( imageWidth*0.25, imageHeight*0.25 );
			topRight    = new Point( imageWidth*0.75, imageHeight*0.25 );
			bottomLeft  = new Point( imageWidth*0.25, imageHeight*0.75 );
			bottomRight = new Point( imageWidth*0.75, imageHeight*0.75 );
			computeTargetSize();
		}

		void computeTargetSize()
		{
			targetHeight = (int) Math.round( Math.max(
					topLeft .computeDist(bottomLeft ),
					topRight.computeDist(bottomRight)
			) );
			targetWidth = (int) Math.round( Math.max(
					   topLeft.computeDist(   topRight),
					bottomLeft.computeDist(bottomRight)
			) );
			
			if (DEBUG_TEST_ENGINE)
				debug_gc = new GeometryComputations();
			
			if (DEBUG_OTHER_OUTPUTS)
				System.out.printf("TargetSize: %d x %d%n", targetWidth, targetHeight);
		}
		
		BufferedImage computeImage()
		{
			return ProgressDialog.runWithProgressDialogRV(window, "Computing Image", 400, this::computeImage);
		}
		
		private BufferedImage computeImage(ProgressDialog pd)
		{
			if (DEBUG_COMPUTE_DUMMY_IMAGE)
			{
				Tools.setIndeterminateTaskTitle(pd, "Create Dummy");
				return createDummy();
			}
			
			long startTime = System.currentTimeMillis();
			
			Tools.setIndeterminateTaskTitle(pd, "Prepare Data");
			
			GeometryComputations gc = new GeometryComputations();
			
			TargetPixel[][] targetRaster = new TargetPixel[targetWidth][targetHeight];
			for (TargetPixel[] row : targetRaster)
				for (int i=0; i<row.length; i++)
					row[i] = new TargetPixel();
			
			Point minSource = Point.min( topLeft, topRight, bottomLeft, bottomRight );
			Point maxSource = Point.max( topLeft, topRight, bottomLeft, bottomRight );
			int minSourceX = (int) Math.floor( minSource.x );
			int minSourceY = (int) Math.floor( minSource.y );
			int maxSourceX = (int) Math.floor( maxSource.x );
			int maxSourceY = (int) Math.floor( maxSource.y );
			
			Tools.setTaskTitle(pd, "Map Source Image to Target Image", 0, 0, maxSourceX-minSourceX);
			
			for (int sourceX=minSourceX; sourceX<=maxSourceX; sourceX++)
			{
				Tools.setTaskValue(pd, sourceX-minSourceX);
				for (int sourceY=minSourceY; sourceY<=maxSourceY; sourceY++)
					if (gc.isCandidate(sourceX,sourceY))
					{
						int rgb = image.getRGB(sourceX, sourceY) & 0xFFFFFF;
						PixelCoord sourcePixel = new PixelCoord(sourceX, sourceY);
						
						for (int iX=0; iX<SUBRASTER_SIZE; iX++)
						{
							double srX = sourceX + iX/(double)SUBRASTER_SIZE + 0.5;
							for (int iY=0; iY<SUBRASTER_SIZE; iY++)
							{
								double srY = sourceY + iY/(double)SUBRASTER_SIZE + 0.5;
								
								PixelCoord targetPixelCoord = gc.computeTargetPixel(srX,srY);
								if (targetPixelCoord!=null && targetPixelCoord.isInside(targetWidth,targetHeight))
								{
									TargetPixel targetPixel = targetRaster[targetPixelCoord.x][targetPixelCoord.y];
									PixelData pixelData = targetPixel.hits.computeIfAbsent(sourcePixel, sp -> new PixelData(rgb));
									pixelData.hits++;
								}
							}
						}
					}
			}
			
			Tools.setTaskTitle(pd, "Create final Image", 0, 0, targetWidth);
			
			BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
			
			for (int targetX=0; targetX<targetWidth; targetX++)
			{
				Tools.setTaskValue(pd, targetX);
				for (int targetY=0; targetY<targetHeight; targetY++)
				{
					TargetPixel targetPixel = targetRaster[targetX][targetY];
					image.setRGB(targetX, targetY, targetPixel.computeColor() | 0xFF000000);
				}
			}
			
			String duration = DateTimeFormatter.getDurationStr_ms(System.currentTimeMillis() - startTime);
			System.out.printf("Image computed in %s%n", duration);
			
			return image;
		}
		
		private class GeometryComputations
		{
			private DistToLine topLine;
			private DistToLine leftLine;
			private DistToLine rightLine;
			private DistToLine bottomLine;

			GeometryComputations()
			{
				topLine    = new DistToLine(topLeft   , topRight   , bottomLeft);
				leftLine   = new DistToLine(topLeft   , bottomLeft , topRight  );
				rightLine  = new DistToLine(topRight  , bottomRight, bottomLeft);
				bottomLine = new DistToLine(bottomLeft, bottomRight, topRight  );
			}
			
			private static class DistToLine
			{
				private Point lp1;
				private Point lp2;
				private double sign;
				private Point v_lp2;
				private double v_lp2_length;

				DistToLine(Point linePoint1, Point linePoint2, Point pointAtPositiveSide)
				{
					this.lp1 = linePoint1;
					this.lp2 = linePoint2;
					v_lp2 = lp2.sub(lp1);
					v_lp2_length = v_lp2.length();
					
					sign = 1;
					if (computeDist(pointAtPositiveSide) < 0)
						sign = -1;
				}

				double computeDist(Point p)
				{
					if (v_lp2_length==0)
						return lp1.computeDist(p);
					
					Point v_p = p.sub(lp1);
					double crossProd = v_p.x*v_lp2.y - v_p.y*v_lp2.x;
					double dist = crossProd / v_lp2_length * sign;
					return dist;
				}
			}
			
			PixelCoord computeTargetPixel(double sourceX, double sourceY)
			{
				Point p = new Point(sourceX, sourceY);
				double distToTop    = topLine   .computeDist(p);
				double distToLeft   = leftLine  .computeDist(p);
				double distToRight  = rightLine .computeDist(p);
				double distToBottom = bottomLine.computeDist(p);
				double sumLeftRight = distToLeft+distToRight;
				double sumTopBottom = distToTop+distToBottom;
				if (sumLeftRight==0 || sumTopBottom==0)
					return null;
				int x = (int) Math.floor( distToLeft / sumLeftRight * targetWidth  );
				int y = (int) Math.floor( distToTop  / sumTopBottom * targetHeight );
				return new PixelCoord(x,y);
			}

			boolean isInside(double sourceX, double sourceY)
			{
				PixelCoord targetPixel = computeTargetPixel(sourceX, sourceY);
				return targetPixel!=null && targetPixel.isInside(targetWidth,targetHeight);
			}

			boolean isCandidate(int sourceX, int sourceY)
			{
				Point p = new Point(sourceX, sourceY);
				if (topLeft    .computeSquaredDist(p) < 4) return true;
				if (topRight   .computeSquaredDist(p) < 4) return true;
				if (bottomLeft .computeSquaredDist(p) < 4) return true;
				if (bottomRight.computeSquaredDist(p) < 4) return true;
				if (isInside(sourceX  , sourceY  )) return true;
				if (isInside(sourceX+1, sourceY  )) return true;
				if (isInside(sourceX  , sourceY+1)) return true;
				if (isInside(sourceX+1, sourceY+1)) return true;
				return false;
			}
		}

		private static class TargetPixel
		{
			final Map<PixelCoord,PixelData> hits = new HashMap<>();

			int computeColor()
			{
				long sumOfHits = hits.values()
						.stream()
						.collect(Collectors.summarizingInt(pd -> pd.hits))
						.getSum();
				if (sumOfHits==0)
					return 0;
				
				double r0 = 0;
				double g0 = 0;
				double b0 = 0;
				for (PixelData pd : hits.values())
				{
					r0 += ((pd.rgb>>16) & 0xFF) * pd.hits / (double)sumOfHits;
					g0 += ((pd.rgb>> 8) & 0xFF) * pd.hits / (double)sumOfHits;
					b0 += ((pd.rgb    ) & 0xFF) * pd.hits / (double)sumOfHits;
				}
				
				int r = (int) Math.min(Math.max(0, Math.round(r0)), 255);
				int g = (int) Math.min(Math.max(0, Math.round(g0)), 255);
				int b = (int) Math.min(Math.max(0, Math.round(b0)), 255);
				return (r << 16) | (g << 8) | b;
			}
		}
		
		private static class PixelData
		{
			final int rgb;
			int hits;
			PixelData(int rgb)
			{
				this.rgb = rgb;
				hits = 0;
			}
		}
		private record PixelCoord(int x, int y)
		{
			boolean isInside(int width, int height)
			{
				return 0<=x && x<width && 0<=y && y<height;
			}
		}

		private BufferedImage createDummy()
		{
			BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = image.createGraphics();
			
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, targetWidth, targetHeight);
			
			g2.setStroke(new BasicStroke(3));
			g2.setColor(Color.RED);
			g2.drawLine(0, 0, targetWidth, targetHeight);
			g2.setColor(Color.GREEN);
			g2.drawLine(targetWidth, 0, 0, targetHeight);
			g2.setColor(Color.BLUE);
			g2.drawOval(0, 0, targetWidth, targetHeight);
			return image;
		}
	}
	
	private static class Point
	{
		double x,y;
		
		Point(double x, double y)
		{
			this.x = x;
			this.y = y;
		}

		void set(double x, double y)
		{
			this.x = x;
			this.y = y;
		}

		void set(Point p)
		{
			this.x = p.x;
			this.y = p.y;
		}
		
		static Point min(Point... points)
		{
			Point min = new Point(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
			for (Point p : points)
			{
				if (min.x > p.x)
					min.x = p.x;
				if (min.y > p.y)
					min.y = p.y;
			}
			return min;
		}
		
		static Point max(Point... points)
		{
			Point max = new Point(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
			for (Point p : points)
			{
				if (max.x < p.x)
					max.x = p.x;
				if (max.y < p.y)
					max.y = p.y;
			}
			return max;
		}

		static Point between(double f, Point p1, Point p2)
		{
			return p2.sub(p1).mul(f).add(p1);
		}

		Point add(Point p)
		{
			return new Point( x+p.x, y+p.y );
		}

		Point sub(Point p)
		{
			return new Point( x-p.x, y-p.y );
		}

		Point mul(double f)
		{
			return new Point( x*f, y*f );
		}

		double length()
		{
			return Math.sqrt(x*x+y*y);
		}

		double computeDist(Point p)
		{
			return Math.sqrt(computeSquaredDist(p));
		}

		double computeSquaredDist(Point p)
		{
			return (x-p.x)*(x-p.x)+(y-p.y)*(y-p.y);
		}
	}
	
	private abstract class ImageEditor
	{
		static final BasicStroke STROKE_DASHED_LINE = new BasicStroke(1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,new float[]{4,6},0);
		
		final String title;
		final boolean needsViewInEditorMode;
		final ImportView.ViewState viewState;
		final ImportView.Handle[] handles;
		ImportView.Handle highlightedHandle;
		java.awt.Point mousePos;
		Point dragDelta;
		boolean isInside;


		ImageEditor(String title, boolean needsViewInEditorMode, ImportView.Handle[] handles)
		{
			this.title = title;
			this.needsViewInEditorMode = needsViewInEditorMode;
			this.viewState = importView.getViewState();
			this.handles = handles;
			highlightedHandle = null;
			mousePos = null;
			dragDelta = null;
			isInside = false;
		}

		protected void setMousePos(MouseEvent e)
		{
			mousePos = e.getPoint();
		}

		protected void updateHandleHighlight(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			highlightedHandle = findNearestHandle(handles, e);
			importView.setCursor(Cursor.getPredefinedCursor(highlightedHandle==null ? Cursor.DEFAULT_CURSOR : highlightedHandle.cursor));
		}

		protected ImportView.Handle findNearestHandle(ImportView.Handle[] handles, MouseEvent e)
		{
			if (handles==null)
				return null;
			
			Point mousePos = viewState.convertPos_ScreenToAngle(e.getPoint());
			double minDist = Double.POSITIVE_INFINITY;
			ImportView.Handle nearestHandle = null;
			
			for (ImportView.Handle handle : handles)
			{
				double squaredDistance = handle.pos.computeSquaredDist(mousePos);
				if (minDist > squaredDistance)
				{
					minDist	= squaredDistance;
					nearestHandle = handle;
				}
			}
			
			if (nearestHandle != null && viewState.convertLength_LengthToScreenF(Math.sqrt(minDist)) < ImportView.Handle.MIN_DISTANCE_SCREEN)
				return nearestHandle;
			
			return null;
		}

		void activate() {}
		abstract BufferedImage getImage();
		protected void drawLines(Graphics2D g2, int x, int y, int width, int height) {}
		protected String getExtraTitleStr() { return null; }

		void mouseMoved(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			updateHandleHighlight(e);
			importView.repaint();
		}

		void mousePressed(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			updateHandleHighlight(e);
			if (highlightedHandle!=null)
				dragDelta = highlightedHandle.pos.sub( viewState.convertPos_ScreenToAngle(e.getPoint()) );
			importView.repaint();
		}

		void mouseDragged(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			if (highlightedHandle!=null && dragDelta!=null)
				highlightedHandle.setPos( viewState.convertPos_ScreenToAngle(e.getPoint()).add(dragDelta) );
			importView.repaint();
		}

		void mouseReleased(MouseEvent e)
		{
			setMousePos(e);
			dragDelta = null;
			importView.repaint();
		}

		void mouseEntered(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			isInside = true;
			importView.repaint();
		}

		void mouseExited(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			isInside = false;
			importView.repaint();
		}

		void drawExtras(Graphics2D g2, int x, int y, int width, int height)
		{
			drawLines(g2, x, y, width, height);
			
			if (handles!=null)
				for (ImportView.Handle handle : handles)
					if (handle!=highlightedHandle)
						handle.draw(viewState, g2, false);
			
			if (highlightedHandle!=null)
			{
				highlightedHandle.draw(viewState, g2, true);
				ImportView.drawTooltip(g2, mousePos, highlightedHandle.title);
			}
		}
	}
	
	private abstract class Scenario
	{
		enum Type { FullImageManipulation, ImageCutOut }
		abstract JPanel createDialogBottomToolbar();
		abstract ImageEditor getInitialEditor();
	}

	private class FullImageManipulation extends Scenario
	{
		private final ShowImportedImage editor;
		private BufferedImage image;
		
		FullImageManipulation()
		{
			image = originalImage;
			editor = new ShowImportedImage();
		}
		
		private class DialogBottomToolbar extends JPanel
		{
			private static final long serialVersionUID = -4770056455976201592L;
			private final JButton btnCutOut  ;
			private final JButton btnRot90   ;
			private final JButton btnRot90CCW;
			private final JButton btnRot180  ;
			private final JButton btnRedSize ;
			private final JButton btnReset   ;
			private final JButton btnOk      ;
			private final JButton btnCancel  ;

			DialogBottomToolbar()
			{
				super(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				add(btnCutOut   = Tools.createButton("Cut Out"      , true, GrayCommandIcons.IconGroup.Cut      , e -> cutOut()            ), c);
				add(btnRot90    = Tools.createButton("90°"          , true, GrayCommandIcons.IconGroup.Reload   , e -> rotateImage90(false)), c);
				add(btnRot90CCW = Tools.createButton("90°"          , true, GrayCommandIcons.IconGroup.ReloadCCW, e -> rotateImage90(true )), c);
				add(btnRot180   = Tools.createButton("180°"         , true, GrayCommandIcons.IconGroup.Reload   , e -> rotateImage180()    ), c);
				add(btnRedSize  = Tools.createButton("Reduce Size"  , true, null, e -> reduceSize()), c);
				add(btnReset    = Tools.createButton("Reset"        , true, null, e -> { image = originalImage; importView.reset(); updateBtns(); }), c);
				c.weightx = 1;
				add(new JLabel(), c);
				c.weightx = 0;
				add(btnOk       = Tools.createButton("Ok"           , true, null, e -> { result = image; closeDialog(); }), c);
				add(btnCancel   = Tools.createButton("Cancel Import", true, null, e -> closeDialog()), c);
				
				updateBtns();
			}
			
			void updateBtns()
			{
				int imageWidth  = image==null ? -1 : image.getWidth();
				int imageHeight = image==null ? -1 : image.getHeight();
				double f = Math.max(imageWidth, imageHeight) / Tools.IMAGE_REDUCTION__MAX_SIZE;
				int reducedWidth  = (int) Math.round( imageWidth  / f );
				int reducedHeight = (int) Math.round( imageHeight / f );
				
				btnRedSize.setText(
						image == null || f <= Tools.IMAGE_REDUCTION__THRESHOLD
							? "Reduce Size"
							: "Reduce Size to %d x %d".formatted(reducedWidth, reducedHeight)
				);
				
				btnCutOut  .setEnabled(image!=null);
				btnRot90   .setEnabled(image!=null);
				btnRot90CCW.setEnabled(image!=null);
				btnRot180  .setEnabled(image!=null);
				btnRedSize .setEnabled(image!=null  && f > Tools.IMAGE_REDUCTION__THRESHOLD);
				btnReset   .setEnabled(image!=null);
				btnOk      .setEnabled(image!=null);
				btnCancel  .setEnabled(true);
			}

			private void reduceSize()
			{
				int imageWidth  = image.getWidth();
				int imageHeight = image.getHeight();
				double f = Math.max(imageWidth, imageHeight) / Tools.IMAGE_REDUCTION__MAX_SIZE;
				if (f > Tools.IMAGE_REDUCTION__THRESHOLD)
				{
					int reducedWidth  = (int) Math.round( imageWidth  / f );
					int reducedHeight = (int) Math.round( imageHeight / f );
					image = ImageView.computeScaledImageByAreaSampling(image, reducedWidth, reducedHeight, true);
					importView.reset();
					updateBtns();
				}
			}

			private void cutOut()
			{
				int width  = ImageImportDialog.this.getWidth();
				int height = ImageImportDialog.this.getHeight();
				BufferedImage result = showDialog(ImageImportDialog.this, image, Scenario.Type.ImageCutOut, width-50, height-100);
				if (result!=null)
				{
					image = result;
					importView.reset();
					updateBtns();
				}
			}

			private void rotateImage180()
			{
				image = ImageTools.rotate180(image);
				importView.reset();
				updateBtns();
			}

			private void rotateImage90(boolean counterClockWise)
			{
				if (counterClockWise)
					image = ImageTools.rotate90_counterclockwise(image);
				else
					image = ImageTools.rotate90_clockwise(image);
				importView.reset(); 
				updateBtns();
			}
		}
		
		@Override
		JPanel createDialogBottomToolbar()
		{
			return new DialogBottomToolbar();
		}

		@Override
		ImageEditor getInitialEditor()
		{
			return editor;
		}

		private class ShowImportedImage extends ImageEditor
		{
			ShowImportedImage()
			{
				super("Imported Image", false, null);
			}

			@Override
			BufferedImage getImage()
			{
				return image;
			}
		}
	}

	private class ImageCutOut extends Scenario
	{
		private final CutOutEngine engine;
		private final DefineRange defineRangeEditor;
		private final ResizeResult resizeResultEditor;
		private ImageEditor currentEditor;
		private DialogBottomToolbar toolbar;
		
		ImageCutOut()
		{
			engine = new CutOutEngine(ImageImportDialog.this, originalImage);
			defineRangeEditor = new DefineRange();
			resizeResultEditor = new ResizeResult();
			currentEditor = defineRangeEditor;
			toolbar = null;
		}
		
		private class DialogBottomToolbar extends JPanel
		{
			private static final long serialVersionUID = 5935836598863163297L;
			private final JComboBox<Integer> cmbbxHitPointCount;
			private final JToggleButton btnDefineRange;
			private final JToggleButton btnResizeResult;
			private final JButton btnSetSize;
			private final JButton btnOk;
			private final JButton btnCancel;

			DialogBottomToolbar()
			{
				super(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				cmbbxHitPointCount = new JComboBox<>(new Integer[] {3,4,5,6,7,8,9,10});
				cmbbxHitPointCount.setRenderer(new Tables.NonStringRenderer<>(obj -> {
					if (obj instanceof Integer n)
						return "%d x %d (%d)".formatted(n,n,n*n);
					return obj==null ? "----" : obj.toString();
				}, 70));
				
				engine.SUBRASTER_SIZE = MyHomeLibrary.appSettings.getInt(MyHomeLibrary.AppSettings.ValueKey.ImageImportDialog_CutOutEngine_SubRasterSize, engine.SUBRASTER_SIZE);
				cmbbxHitPointCount.setSelectedItem(engine.SUBRASTER_SIZE);
				
				cmbbxHitPointCount.addActionListener(e -> {
					Integer size = cmbbxHitPointCount.getItemAt(cmbbxHitPointCount.getSelectedIndex());
					if (size == null) return;
					engine.SUBRASTER_SIZE = size;
					MyHomeLibrary.appSettings.putInt(MyHomeLibrary.AppSettings.ValueKey.ImageImportDialog_CutOutEngine_SubRasterSize, engine.SUBRASTER_SIZE);
					if (currentEditor==resizeResultEditor)
						SwingUtilities.invokeLater(() -> {
							resizeResultEditor.updateImage();
							importView.reset();
						});
				});
				
				btnOk = Tools.createButton("Ok", currentEditor==resizeResultEditor, null, e -> {
					if (currentEditor!=resizeResultEditor) return;
					result = resizeResultEditor.resultImage;
					closeDialog();
				});
				btnCancel = Tools.createButton("Cancel Cut Out", true, null, e -> closeDialog());
				
				ButtonGroup bg = new ButtonGroup();
				btnDefineRange = Tools.createToggleButton("Define Range" , true, currentEditor == defineRangeEditor, bg, null, b -> {
					setEditor(currentEditor = b ? defineRangeEditor : resizeResultEditor);
					updateBtns();
				});
				btnResizeResult = Tools.createToggleButton("Resize Result", true, currentEditor == resizeResultEditor, bg, null, b -> {
					setEditor(currentEditor = b ? resizeResultEditor : defineRangeEditor);
					updateBtns();
				});
				
				btnSetSize = Tools.createButton("Set Size", true, null, e -> reduceSize());
				
				add(new JLabel("Measurement Points per Pixel:  "), c);
				add(cmbbxHitPointCount, c);
				add(btnDefineRange, c);
				add(btnResizeResult, c);
				add(btnSetSize, c);
				c.weightx = 1;
				add(new JLabel(), c);
				c.weightx = 0;
				add(btnOk, c);
				add(btnCancel, c);
				
				updateBtns();
			}

			private void updateBtns()
			{
				int imageWidth  = engine.targetWidth;
				int imageHeight = engine.targetHeight;
				double f = Math.max(imageWidth, imageHeight) / Tools.IMAGE_REDUCTION__MAX_SIZE;
				int reducedWidth  = (int) Math.round( imageWidth  / f );
				int reducedHeight = (int) Math.round( imageHeight / f );
				
				btnSetSize.setText(
						currentEditor!=resizeResultEditor || f <= Tools.IMAGE_REDUCTION__THRESHOLD
							? "Set Size"
							: "Set Size to %d x %d".formatted(reducedWidth, reducedHeight)
				);
				
				btnOk     .setEnabled(currentEditor==resizeResultEditor);
				btnSetSize.setEnabled(currentEditor==resizeResultEditor && f > Tools.IMAGE_REDUCTION__THRESHOLD);
			}

			private void reduceSize()
			{
				if (currentEditor!=resizeResultEditor)
					return;
				
				int imageWidth  = engine.targetWidth ;
				int imageHeight = engine.targetHeight;
				double f = Math.max(imageWidth, imageHeight) / Tools.IMAGE_REDUCTION__MAX_SIZE;
				
				if (f <= Tools.IMAGE_REDUCTION__THRESHOLD)
					return;
				
				engine.targetWidth  = (int) Math.round( imageWidth  / f );
				engine.targetHeight = (int) Math.round( imageHeight / f );
				resizeResultEditor.updateImage();
				importView.reset();
				updateBtns();
			}
		}
		
		@Override
		JPanel createDialogBottomToolbar()
		{
			return toolbar = new DialogBottomToolbar();
		}

		@Override
		ImageEditor getInitialEditor()
		{
			return currentEditor;
		}
		
		private class DefineRange extends ImageEditor
		{
			DefineRange()
			{
				super(
						"Define Range", true,
						new ImportView.Handle[] {
								new ImportView.Handle("Top-Left"    +" Corner", Cursor.MOVE_CURSOR, engine.topLeft    .x, engine.topLeft    .y, engine.topLeft    ::set),
								new ImportView.Handle("Top-Right"   +" Corner", Cursor.MOVE_CURSOR, engine.topRight   .x, engine.topRight   .y, engine.topRight   ::set),
								new ImportView.Handle("Bottom-Left" +" Corner", Cursor.MOVE_CURSOR, engine.bottomLeft .x, engine.bottomLeft .y, engine.bottomLeft ::set),
								new ImportView.Handle("Bottom-Right"+" Corner", Cursor.MOVE_CURSOR, engine.bottomRight.x, engine.bottomRight.y, engine.bottomRight::set)
						}
				);
			}
			
			@Override
			BufferedImage getImage()
			{
				return engine.image;
			}

			@Override
			void mouseMoved(MouseEvent e)
			{
				super.mouseMoved(e);
				if (DEBUG_TEST_ENGINE)
					computeAndShowTestValues(e);
			}
		
			private void computeAndShowTestValues(MouseEvent e)
			{
				if (viewState.isOk() && engine.debug_gc!=null)
				{
					java.awt.Point mouse = e.getPoint();
					Point p = viewState.convertPos_ScreenToAngle(mouse);
					
					double dist2Top    = Tools.callChecked("dist2Top"   , 0.0, () -> engine.debug_gc.   topLine.computeDist(p) );
					double dist2Left   = Tools.callChecked("dist2Left"  , 0.0, () -> engine.debug_gc.  leftLine.computeDist(p) );
					double dist2Right  = Tools.callChecked("dist2Right" , 0.0, () -> engine.debug_gc. rightLine.computeDist(p) );
					double dist2Bottom = Tools.callChecked("dist2Bottom", 0.0, () -> engine.debug_gc.bottomLine.computeDist(p) );
					
					CutOutEngine.PixelCoord targetPixel = Tools.callChecked("computeTargetPixel", null, () -> engine.debug_gc.computeTargetPixel(p.x, p.y));
					
					boolean isInside = Tools.callChecked("isInside", false, () -> engine.debug_gc.isInside(p.x, p.y) );
					
					int x = (int) Math.floor( p.x );
					int y = (int) Math.floor( p.y );
					boolean isCandidate = Tools.callChecked("isCandidate", false, () -> engine.debug_gc.isCandidate(x, y) );
					
					System.out.printf(Locale.ENGLISH, "Engine-Test:"
							+ " mouse:%10s -> source:%15s"
							+ " -> {"
							+ " targetPixel:%10s, isInside:%5s, isCandidate:%5s,"
							+ " top:%8.2f,"
							+ " left:%8.2f,"
							+ " right:%8.2f,"
							+ " bottom:%8.2f"
							+ " } %n",
							toString(mouse), toString(p),
							toString(targetPixel), isInside, isCandidate,
							dist2Top,
							dist2Left,
							dist2Right,
							dist2Bottom
					);
				}
			}
		
			private Object toString(CutOutEngine.PixelCoord p) // max length: 10
			{
				if (p==null)
					return "<null>";
				return "%dx%d".formatted(p.x,p.y);
			}
		
			private String toString(Point p) // max length: 15 
			{
				if (p==null)
					return "<null>";
				return String.format(Locale.ENGLISH, "%1.2fx%1.2f", p.x, p.y);
			}
		
			private String toString(java.awt.Point p) // max length: 10
			{
				if (p==null)
					return "<null>";
				return "%dx%d".formatted(p.x,p.y);
			}
		
			@Override
			void mouseReleased(MouseEvent e)
			{
				if (dragDelta != null)
					engine.computeTargetSize();
				super.mouseReleased(e);
			}
		
			@Override
			protected void drawLines(Graphics2D g2, int x, int y, int width, int height)
			{
				if (isInside || dragDelta!=null)
				{
					Stroke prevStroke = g2.getStroke();
					
					g2.setColor(Color.BLACK);
					g2.setXORMode(Color.WHITE);
					
					Point topLeft     = viewState.convertPos_AngleToScreen(engine.topLeft    );
					Point topRight    = viewState.convertPos_AngleToScreen(engine.topRight   );
					Point bottomLeft  = viewState.convertPos_AngleToScreen(engine.bottomLeft );
					Point bottomRight = viewState.convertPos_AngleToScreen(engine.bottomRight);
					
					Point top1 = Point.between(1/3.0, topLeft, topRight);
					Point top2 = Point.between(2/3.0, topLeft, topRight);
					Point left1 = Point.between(1/3.0, topLeft, bottomLeft);
					Point left2 = Point.between(2/3.0, topLeft, bottomLeft);
					Point right1 = Point.between(1/3.0, topRight, bottomRight);
					Point right2 = Point.between(2/3.0, topRight, bottomRight);
					Point bottom1 = Point.between(1/3.0, bottomLeft, bottomRight);
					Point bottom2 = Point.between(2/3.0, bottomLeft, bottomRight);
					
					ImportView.drawLine(g2,    topLeft,    topRight);
					ImportView.drawLine(g2, bottomLeft, bottomRight);
					ImportView.drawLine(g2,    topLeft,  bottomLeft);
					ImportView.drawLine(g2,   topRight, bottomRight);
					
					g2.setStroke(STROKE_DASHED_LINE);
					ImportView.drawLine(g2, top1, bottom1);
					ImportView.drawLine(g2, top2, bottom2);
					ImportView.drawLine(g2, left1, right1);
					ImportView.drawLine(g2, left2, right2);
					
					g2.setPaintMode();
					g2.setStroke(prevStroke);
				}
			}
		}

		private class ResizeResult extends ImageEditor
		{
			private BufferedImage resultImage;
			private boolean isComputing;
		
			ResizeResult()
			{
				super("Resize Result", true, new Handles(engine).toArray());
				resultImage = null;
				isComputing = false;
			}
			
			@Override
			protected String getExtraTitleStr()
			{
				if (isComputing)
					return "computing image %d x %d ...".formatted(engine.targetWidth, engine.targetHeight);
				return "%d x %d".formatted(engine.targetWidth, engine.targetHeight);
			}
		
			@Override
			void activate()
			{
				updateImage();
			}
		
			@Override
			BufferedImage getImage()
			{
				return resultImage;
			}
			
			@Override
			void mouseDragged(MouseEvent e)
			{
				super.mouseDragged(e);
				updateWindowTitle();
				if (toolbar!=null)
					toolbar.updateBtns();
			}
		
			@Override
			void mouseReleased(MouseEvent e)
			{
				if (dragDelta != null)
				{
					updateImage();
					importView.reset();
					if (toolbar!=null)
						toolbar.updateBtns();
				}
				super.mouseReleased(e);
			}
		
			private void updateImage()
			{
				isComputing = true; updateWindowTitle();
				resultImage = engine.computeImage();
				isComputing = false; updateWindowTitle();
				Handles.resetHandles(engine, handles);
			}
		
			@Override
			protected void drawLines(Graphics2D g2, int x, int y, int width, int height)
			{
				if (isInside || dragDelta!=null)
				{
					Stroke prevStroke = g2.getStroke();
					
					g2.setColor(Color.BLACK);
					g2.setXORMode(Color.WHITE);
					
					int x1 = viewState.convertPos_AngleToScreen_LongX(0);
					int y1 = viewState.convertPos_AngleToScreen_LatY (0);
					int x2 = viewState.convertPos_AngleToScreen_LongX(engine.targetWidth );
					int y2 = viewState.convertPos_AngleToScreen_LatY (engine.targetHeight);
					
					g2.drawLine(x1,y1,x2,y1);
					g2.drawLine(x1,y2,x2,y2);
					g2.drawLine(x1,y1,x1,y2);
					g2.drawLine(x2,y1,x2,y2);
					
					g2.setStroke(STROKE_DASHED_LINE);
					g2.drawLine(x1,y1,x2,y2);
					g2.drawLine(x1,y2,x2,y1);
					
					g2.setPaintMode();
					g2.setStroke(prevStroke);
				}
			}
		
			private static class Handles
			{
				private final SpecHandle handleHeight;
				private final SpecHandle handleWidth;
				private final SpecHandle handleBoth;
				private final CutOutEngine engine;
			
				Handles(CutOutEngine engine)
				{
					this.engine = engine;
					handleWidth  = new SpecHandle("Width"         , Cursor. E_RESIZE_CURSOR, p -> p.set( engine.targetWidth    , engine.targetHeight/2.0 ), this::changeWidth );
					handleHeight = new SpecHandle("Height"        , Cursor. S_RESIZE_CURSOR, p -> p.set( engine.targetWidth/2.0, engine.targetHeight     ), this::changeHeight);
					handleBoth   = new SpecHandle("Width & Height", Cursor.SE_RESIZE_CURSOR, p -> p.set( engine.targetWidth    , engine.targetHeight     ), this::changeBoth  );
				}
				
				private static class SpecHandle extends ImportView.Handle
				{
					private final Consumer<Point> resetPos;
		
					SpecHandle(String title, int cursor, Consumer<Point> resetPos, Consumer<Point> setValue)
					{
						super(title, cursor, 0, 0, setValue);
						this.resetPos = resetPos;
						resetPos();
					}
		
					void resetPos()
					{
						resetPos.accept(pos);
					}
				}
			
				ImportView.Handle[] toArray()
				{
					return new ImportView.Handle[] {
							handleWidth,
							handleHeight,
							handleBoth
					};
				}
				
				static void resetHandles(CutOutEngine engine, ImportView.Handle[] handles)
				{
					if (handles==null) return;
					for (ImportView.Handle handle : handles)
						if (handle instanceof SpecHandle specHandle)
							specHandle.resetPos();
				}
			
				private void changeBoth(Point p)
				{
					engine.targetWidth  = (int) Math.round( p.x );
					engine.targetHeight = (int) Math.round( p.y );
					handleWidth .resetPos();
					handleHeight.resetPos();
					handleBoth  .resetPos();
				}
			
				private void changeWidth(Point p)
				{
					engine.targetWidth = (int) Math.round( p.x );
					handleWidth .resetPos();
					handleHeight.resetPos();
					handleBoth  .resetPos();
				}
			
				private void changeHeight(Point p)
				{
					engine.targetHeight = (int) Math.round( p.y );
					handleWidth .resetPos();
					handleHeight.resetPos();
					handleBoth  .resetPos();
				}
			}
		}
	} 

	@SuppressWarnings("unused")
	private static class ImportView extends ZoomableCanvas<ImportView.ViewState>
	{
		private static final Color TOOLTIP_TEXT   = Color.BLACK;
		private static final Color TOOLTIP_FILL   = new Color(0xFFFFE1);
		private static final Color TOOLTIP_BORDER = new Color(0x646464);
		private static final long serialVersionUID = 8848334775743934506L;
		private static final Color COLOR_AXIS = new Color(0x70000000,true);
		
		private ImageEditor editor;

		ImportView()
		{
			this.editor = null;
			activateMapScale(COLOR_AXIS, "px", true);
			activateAxes(COLOR_AXIS, true,true,true,true);
		}
		
		ViewState getViewState()
		{
			return viewState;
		}

		@Override public void mousePressed (MouseEvent e) { super.mousePressed (e); if (editor!=null) editor.mousePressed (e); }
		@Override public void mouseDragged (MouseEvent e) { super.mouseDragged (e); if (editor!=null) editor.mouseDragged (e); }
		@Override public void mouseReleased(MouseEvent e) { super.mouseReleased(e); if (editor!=null) editor.mouseReleased(e); }
		@Override public void mouseEntered (MouseEvent e) { super.mouseEntered (e); if (editor!=null) editor.mouseEntered (e); }
		@Override public void mouseMoved   (MouseEvent e) { super.mouseMoved   (e); if (editor!=null) editor.mouseMoved   (e); }
		@Override public void mouseExited  (MouseEvent e) { super.mouseExited  (e); if (editor!=null) editor.mouseExited  (e); }

		static void drawLine(Graphics2D g2, Point p1, Point p2)
		{
			drawLineD(g2, p1.x, p1.y, p2.x, p2.y);
		}

		static void drawLineD(Graphics2D g2, double x1, double y1, double x2, double y2)
		{
			drawLine(g2, (int)Math.round(x1), (int)Math.round(y1), (int)Math.round(x2), (int)Math.round(y2));
		}

		static void drawLine(Graphics2D g2, int x1, int y1, int x2, int y2)
		{
			g2.drawLine(x1, y1, x2, y2);
		}

		static void drawTooltip(Graphics2D g2, java.awt.Point pos, String title)
		{
			if (pos==null) return;
			
			Rectangle2D bounds = g2.getFontMetrics().getStringBounds(title, g2);
			
			int textX = pos.x+10;
			int textY = pos.y+20;
			int boxX = (int)Math.round( textX+bounds.getX() ) - 3;
			int boxY = (int)Math.round( textY+bounds.getY() );
			int boxW = (int)Math.round( bounds.getWidth () ) + 6;
			int boxH = (int)Math.round( bounds.getHeight() ) + 2;
			
			g2.setColor(TOOLTIP_BORDER);
			g2.drawRect(boxX-1, boxY-1, boxW+1, boxH+1);
			
			g2.setColor(TOOLTIP_FILL);
			g2.fillRect(boxX, boxY, boxW, boxH);
			
			g2.setColor(TOOLTIP_TEXT);
			g2.drawString(title, textX, textY);
		}

		static void drawRectD(Graphics2D g2, double x, double y, double width, double height)
		{
			g2.drawRect((int)Math.round(x), (int)Math.round(y), (int)Math.round(width), (int)Math.round(height));
		}

		static void fillRectD(Graphics2D g2, double x, double y, double width, double height)
		{
			g2.fillRect((int)Math.round(x), (int)Math.round(y), (int)Math.round(width), (int)Math.round(height));
		}

		void setEditor(ImageEditor editor)
		{
			this.editor = editor;
			reset();
			if (this.editor!=null && this.editor.needsViewInEditorMode)
				activateEditorMode();
			else
				activateViewerMode();
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height)
		{
			if (viewState.isOk() && g instanceof Graphics2D g2)
			{
				Shape prevClip = g2.getClip();
				g2.setClip(new Rectangle(x, y, width, height));
				
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
				
				if (editor!=null)
				{
					BufferedImage image = editor.getImage();
					if (image!=null)
					{
						int imageX      = viewState.convertPos_AngleToScreen_LongX(0);
						int imageY      = viewState.convertPos_AngleToScreen_LatY (0);
						int imageWidth  = viewState.convertPos_AngleToScreen_LongX(image.getWidth ()) - imageX;
						int imageHeight = viewState.convertPos_AngleToScreen_LatY (image.getHeight()) - imageY;
						g2.drawImage(image, imageX, imageY, imageWidth, imageHeight, null);
					}
					
					editor.drawExtras(g2, x, y, width, height);
				}
				
				drawMapDecoration(g2, x, y, width, height);
				
				g2.setClip(prevClip);
			}
		}

		@Override protected ViewState createViewState() { return new ViewState(); }

		class ViewState extends ZoomableCanvas.ViewState {
			
			ViewState() {
				super(ImportView.this,0.1f);
				setPlainMapSurface();
				setVertAxisDownPositive(true);
				//debug_showChanges_scalePixelPerLength = true;
			}

			Point convertPos_AngleToScreen(Point p)
			{
				return new Point(
						convertPos_AngleToScreen_LongXf(p.x),
						convertPos_AngleToScreen_LatYf (p.y)
				);
			}

			Point convertPos_ScreenToAngle(java.awt.Point p)
			{
				return convertPos_ScreenToAngle(p.x, p.y);
			}

			Point convertPos_ScreenToAngle(int x, int y)
			{
				return new Point(
						convertPos_ScreenToAngle_LongX(x),
						convertPos_ScreenToAngle_LatY (y)
				);
			}

			@Override
			protected void determineMinMax(MapLatLong min, MapLatLong max) {
				BufferedImage image = editor==null ? null : editor.getImage();
				min.longitude_x = 0.0;
				min.latitude_y  = 0.0;
				max.longitude_x = image==null ? 100.0 : image.getWidth ();
				max.latitude_y  = image==null ? 100.0 : image.getHeight();
			}
		}
		
		static class Handle
		{
			public  static final int MIN_DISTANCE_SCREEN = 10;
			private static final Color BORDERCOLOR_NORMAL = TOOLTIP_TEXT;
			private static final Color FILLCOLOR_NORMAL = Color.GREEN;
			private static final Color FILLCOLOR_HIGHLIGHTED = TOOLTIP_FILL;
			static final int RADIUS = 3;
			
			final String title;
			final int cursor;
			final Point pos;
			final Consumer<Point> setValue;
			
			Handle(String title, int cursor, double x, double y, Consumer<Point> setValue)
			{
				this.title = title;
				this.cursor = cursor;
				this.setValue = setValue;
				pos = new Point(x, y);
			}

			public void setPos(Point p)
			{
				pos.set(p);
				setValue.accept(p);
			}

			public void draw(ViewState viewState, Graphics2D g2, boolean isHighlighted)
			{
				Point screenPos = viewState.convertPos_AngleToScreen(pos);
				int x = (int) Math.round( screenPos.x );
				int y = (int) Math.round( screenPos.y );
				
				g2.setColor(isHighlighted ? FILLCOLOR_HIGHLIGHTED : FILLCOLOR_NORMAL);
				g2.fillOval(x-RADIUS+1, y-RADIUS+1, 2*RADIUS-1, 2*RADIUS-1);
				g2.setColor(BORDERCOLOR_NORMAL);
				g2.drawOval(x-RADIUS, y-RADIUS, 2*RADIUS, 2*RADIUS);
			}
		}
	}
}
