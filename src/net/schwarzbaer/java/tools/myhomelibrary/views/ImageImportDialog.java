package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import javax.swing.JButton;

import net.schwarzbaer.java.lib.gui.ImageViewDialog;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.ZoomableCanvas;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;

public class ImageImportDialog extends StandardDialog
{
	private static final long serialVersionUID = -6506180541375218481L;
	private final Engine engine;
	private final Step[] steps;
	private final ImportView importView;
	private final JButton btnPrevStep;
	private final JButton btnNextStep;
	private final JButton btnOk;
	private BufferedImage result;
	private int currentStep;

	ImageImportDialog(Window window, BufferedImage originalImage)
	{
		super(window, "Image Import");
		result = null;
		
		importView = new ImportView();
		engine = new Engine(originalImage);
		steps = new Step[] {
				new Step.ShowImportedImage(engine, importView),
				new Step.DefineRange      (engine, importView),
				new Step.ResizeResult     (engine, importView)
		};
		currentStep = 0;
		
		createGUI(
				importView,
				btnPrevStep = Tools.createButton("<< Previous Step", true, null, e -> switchStep(-1)),
				btnNextStep = Tools.createButton("Next Step >>"    , true, null, e -> switchStep(+1)),
				btnOk       = Tools.createButton("Ok"              , true, null, e -> {}),
				Tools.createButton("Close", true, null, e -> closeDialog())
		);
	}

	@Override
	public void showDialog()
	{
		switchStep(-currentStep); // return to step 0
		super.showDialog(Position.PARENT_CENTER);
	}

	private void switchStep(int inc)
	{
		System.out.printf("switchStep( %d -> %d )%n", currentStep, currentStep+inc);
		if (currentStep+inc<0 || currentStep+inc>=steps.length)
			return;
		
		currentStep += inc;
		Step step = steps[currentStep];
		step.activate();
		setTitle("Image Import - %s".formatted(step.title));
		importView.setStep(step);
		btnPrevStep.setEnabled(0 < currentStep);
		btnNextStep.setEnabled(currentStep+1 < steps.length);
		btnOk      .setEnabled(currentStep+1 == steps.length);
	}

	public static void test(Window window)
	{
		BufferedImage image = FileIO.loadImagefile("import.test.jpg");
		//new ImageViewDialog(window, image, "Loaded Image", 800, 500).setVisible(true);
		
		ImageImportDialog dlg = new ImageImportDialog(window, image);
		dlg.showDialog();
		
		new ImageViewDialog(window, dlg.result, "Result Image", 500, 800, true).setVisible(true);
		System.exit(0);
	}
	
	private static class Engine
	{
		private final BufferedImage originalImage;
		private final Point topLeft;
		private final Point topRight;
		private final Point bottomLeft;
		private final Point bottomRight;
		private int targetHeight;
		private int targetWidth;
		
		Engine(BufferedImage originalImage)
		{
			this.originalImage = originalImage;
			int imageWidth  = originalImage.getWidth ();
			int imageHeight = originalImage.getHeight();
			
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
		}

		BufferedImage computeImage()
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
			
			// TODO: Engine.computeImage()
			
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

		double computeDist(Point p)
		{
			return Math.sqrt(computeSquaredDist(p));
		}

		private double computeSquaredDist(Point p)
		{
			return (x-p.x)*(x-p.x)+(y-p.y)*(y-p.y);
		}
	}
	
	private static class Step
	{
		private static final BasicStroke STROKE_DASHED_LINE = new BasicStroke(1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,new float[]{4,6},0);
		
		final String title;
		final Engine engine;
		final boolean isEditor;
		final ImportView view;
		final ImportView.ViewState viewState;
		final ImportView.Handle[] handles;
		ImportView.Handle highlightedHandle;
		java.awt.Point mousePos;
		Point dragDelta;
		boolean isInside;

		Step(String title, boolean isEditor, Engine engine, ImportView view, ImportView.Handle[] handles)
		{
			this.title = title;
			this.isEditor = isEditor;
			this.engine = engine;
			this.view = view;
			this.viewState = view.getViewState();
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
			view.setCursor(Cursor.getPredefinedCursor(highlightedHandle==null ? Cursor.DEFAULT_CURSOR : highlightedHandle.cursor));
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
		BufferedImage getImage() { return engine.originalImage; }
		protected void drawLines(Graphics2D g2, int x, int y, int width, int height) {}

		void mouseMoved(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			updateHandleHighlight(e);
			view.repaint();
		}

		void mousePressed(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			updateHandleHighlight(e);
			if (highlightedHandle!=null)
				dragDelta = highlightedHandle.pos.sub( viewState.convertPos_ScreenToAngle(e.getPoint()) );
			view.repaint();
		}

		void mouseDragged(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			if (highlightedHandle!=null && dragDelta!=null)
				highlightedHandle.setPos( viewState.convertPos_ScreenToAngle(e.getPoint()).add(dragDelta) );
			view.repaint();
		}

		void mouseReleased(MouseEvent e)
		{
			setMousePos(e);
			dragDelta = null;
			view.repaint();
		}

		void mouseEntered(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			isInside = true;
			view.repaint();
		}

		void mouseExited(MouseEvent e)
		{
			if (!viewState.isOk()) return;
			setMousePos(e);
			isInside = false;
			view.repaint();
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

		static class ShowImportedImage extends Step
		{
			ShowImportedImage(Engine engine, ImportView view)
			{
				super("Imported Image", false, engine, view, null);
			}
		}

		static class DefineRange extends Step
		{
			DefineRange(Engine engine, ImportView view)
			{
				super(
						"Define Range", true,
						engine, view,
						new ImportView.Handle[] {
								new ImportView.Handle("Top-Left"    +" Corner", Cursor.MOVE_CURSOR, engine.topLeft    .x, engine.topLeft    .y, engine.topLeft    ::set),
								new ImportView.Handle("Top-Right"   +" Corner", Cursor.MOVE_CURSOR, engine.topRight   .x, engine.topRight   .y, engine.topRight   ::set),
								new ImportView.Handle("Bottom-Left" +" Corner", Cursor.MOVE_CURSOR, engine.bottomLeft .x, engine.bottomLeft .y, engine.bottomLeft ::set),
								new ImportView.Handle("Bottom-Right"+" Corner", Cursor.MOVE_CURSOR, engine.bottomRight.x, engine.bottomRight.y, engine.bottomRight::set)
						}
				);
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

		static class ResizeResult extends Step
		{
			private BufferedImage image;

			ResizeResult(Engine engine, ImportView view)
			{
				super("Resize Result", true, engine, view, new Handles(engine).toArray());
				image = null;
			}
			
			@Override
			void activate()
			{
				image = engine.computeImage();
				Handles.resetHandles(engine, handles);
			}

			@Override
			BufferedImage getImage()
			{
				return image;
			}

			@Override
			void mouseReleased(MouseEvent e)
			{
				if (dragDelta != null)
				{
					image = engine.computeImage();
					Handles.resetHandles(engine, handles);
				}
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
				private final Engine engine;
			
				Handles(Engine engine)
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
				
				static void resetHandles(Engine engine, ImportView.Handle[] handles)
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
		
		private Step step;

		ImportView()
		{
			this.step = null;
			activateMapScale(COLOR_AXIS, "px", true);
			activateAxes(COLOR_AXIS, true,true,true,true);
			setPreferredSize(600, 600);
		}
		
		ViewState getViewState()
		{
			return viewState;
		}

		@Override public void mousePressed (MouseEvent e) { super.mousePressed (e); if (step!=null) step.mousePressed (e); }
		@Override public void mouseDragged (MouseEvent e) { super.mouseDragged (e); if (step!=null) step.mouseDragged (e); }
		@Override public void mouseReleased(MouseEvent e) { super.mouseReleased(e); if (step!=null) step.mouseReleased(e); }
		@Override public void mouseEntered (MouseEvent e) { super.mouseEntered (e); if (step!=null) step.mouseEntered (e); }
		@Override public void mouseMoved   (MouseEvent e) { super.mouseMoved   (e); if (step!=null) step.mouseMoved   (e); }
		@Override public void mouseExited  (MouseEvent e) { super.mouseExited  (e); if (step!=null) step.mouseExited  (e); }

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

		void setStep(Step step)
		{
			this.step = step;
			reset();
			if (this.step!=null && this.step.isEditor)
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
				
				if (step!=null)
				{
					BufferedImage image = step.getImage();
					if (image!=null)
					{
						int imageX      = viewState.convertPos_AngleToScreen_LongX(0);
						int imageY      = viewState.convertPos_AngleToScreen_LatY (0);
						int imageWidth  = viewState.convertPos_AngleToScreen_LongX(image.getWidth ()) - imageX;
						int imageHeight = viewState.convertPos_AngleToScreen_LatY (image.getHeight()) - imageY;
						g2.drawImage(image, imageX, imageY, imageWidth, imageHeight, null);
					}
					
					step.drawExtras(g2, x, y, width, height);
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
				BufferedImage image = step==null ? null : step.getImage();
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
