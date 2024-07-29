package github.io.slimedleviathan;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModTab extends CreativeModeTab{

	private final ItemStack icon;
	
	public ModTab(int index, String name, ItemStack icon) {
		super(index, name);
		
		this.icon = icon;
	}

	@Override
	public ItemStack makeIcon() { // returns the item that represents the tab
		return icon;
	}

}
