package io.github.zom.component;

import com.artemis.Component;
import io.github.zom.util.Inventory;

public class InventoryComponent extends Component {
    public Inventory inventory;
    public void init(int rows, int cols) { inventory = new Inventory(rows, cols); }
}
