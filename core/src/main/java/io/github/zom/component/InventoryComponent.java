package io.github.zom.component;

import com.artemis.Component;

import io.github.zom.util.Inventory;

/**
 * Attaches an Inventory grid to any entity (player, container, vehicle …).
 * The UI layer reads this to draw inventory slots.
 */
public class InventoryComponent extends Component {

    public Inventory inventory;

    /** Create the grid with the given dimensions. */
    public void init(int rows, int cols) {
        inventory = new Inventory(rows, cols);
    }
}
