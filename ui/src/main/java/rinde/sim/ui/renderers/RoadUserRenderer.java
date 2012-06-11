/**
 * 
 */
package rinde.sim.ui.renderers;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> changes in
 *         handling colors
 * 
 */
public class RoadUserRenderer implements ModelRenderer<RoadModel> {

	protected RoadModel rs;
	protected boolean useEncirclement;
	private final UiSchema uiSchema;

	public RoadUserRenderer() {
		this(new UiSchema(false), false);
	}

	public RoadUserRenderer(UiSchema schema, boolean useEncirclement) {
		this.useEncirclement = useEncirclement;
		uiSchema = schema == null ? new UiSchema(false) : schema;
	}

	@Override
	public void renderDynamic(GC gc, ViewPort vp) {
		final int radius = 4;
		final int outerRadius = 10;
		uiSchema.initialize();
		gc.setBackground(uiSchema.getDefaultColor());

		Map<RoadUser, Point> objects = rs.getObjectsAndPositions();
		synchronized (objects) {
			for (Entry<RoadUser, Point> entry : objects.entrySet()) {
				Point p = entry.getValue();
				Class<?> type = entry.getKey().getClass();
				final Image image = uiSchema.getImage(type);
				final int x = (int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale) - radius;
				final int y = (int) (vp.origin.y + (p.y - vp.rect.min.y) * vp.scale) - radius;
				if (image != null) {
					int offsetX = x - image.getBounds().width / 2;
					int offsetY = y - image.getBounds().height / 2;
					gc.drawImage(image, offsetX, offsetY);
				} else {
					final Color color = uiSchema.getColor(type);
					if (color == null) {
						continue;
					}
					gc.setBackground(color);
					if (useEncirclement) {
						gc.setForeground(gc.getBackground());
						gc.drawOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale) - outerRadius, (int) (vp.origin.y + (p.y - vp.rect.min.y)
								* vp.scale)
								- outerRadius, 2 * outerRadius, 2 * outerRadius);
					}
					gc.fillOval((int) (vp.origin.x + (p.x - vp.rect.min.x) * vp.scale) - radius, (int) (vp.origin.y + (p.y - vp.rect.min.y)
							* vp.scale)
							- radius, 2 * radius, 2 * radius);
				}

			}
		}
	}

	@Override
	public void renderStatic(GC gc, ViewPort vp) {}

	@Override
	public ViewRect getViewRect() {
		return null;
	}

	@Override
	public void register(RoadModel model) {
		rs = model;
	}

	@Override
	public Class<RoadModel> getSupportedModelType() {
		return RoadModel.class;
	}

}