package github.io.slimedleviathan;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Event.HasResult;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

// so this is the 
public class MainItem extends Item{

	public static String ITEM_DATA_KEY = "capturecraft.item_keys";
	public static String HELD_ENTITY_KEY = "capturecraft.held_entity";
	
	public MainItem(Item.Properties itemProperties) {
		super(itemProperties.stacksTo(1)); // always no matter what stack to only 1
	}

	// function that allows the player to place a mob
	@Override
	public InteractionResult useOn(UseOnContext context) {
		// do something here if the item has an entity in it
		
		if (context.getLevel().getClass() == ServerLevel.class) {
			
			var clickPos = context.getClickLocation();
			
			CaptureCraft.LOGGER.info("Tag: " + context.getItemInHand().getTag().getAllKeys().toString());
			
			var item = context.getItemInHand();
			
			CompoundTag itemData = new CompoundTag(); // this will be placed onto the item at the end of this if statement
			
			CompoundTag allData = item.getTag();
			
			if (allData == null) {
				allData = new CompoundTag();
			}
			
			String entityLocString = null;
			
			if (allData.contains(HELD_ENTITY_KEY)) {
				entityLocString = allData.get(HELD_ENTITY_KEY).getAsString();
			}
			else {
				entityLocString = null;
			}
			
			// get the previous keys of the item (if they are there)
			if (allData.contains(ITEM_DATA_KEY)) {
				String[] itemDataKeys = item.getTag().get(ITEM_DATA_KEY).getAsString().split(", ");
				
				// for all of the tags keys in the variable, we take the data and merge it into the itemData
				for (String key : itemDataKeys) {
					itemData.merge(allData.getCompound(key));
					
					// remove the key from allData, so we dont use it later for a mob
					allData.remove(key);
				}
			}
			
			// now that either the item didnt have any nbt tags before, and therefore we only have mobData, or we removed the item's nbt tags
			// we just put all data onto the mob that spawns and hope that the item didnt have any same keys as the mob
			
			for (String key : allData.getAllKeys()) {
				CaptureCraft.LOGGER.info(key + ": " + allData.get(key).getAsString());
			}
			
			item.setTag(itemData);
			
			// we need to make sure that the held_entity tag has information, take and remove it from the data
			// it allows us to make sure the wand has an entity in it and later allows us to place said entity
			if (entityLocString != null && !entityLocString.isBlank()) {
				String[] entityLoc = entityLocString.split(":");
				
				String namespace = entityLoc[0];
				String path = entityLoc[1];
				
				allData.remove(HELD_ENTITY_KEY); // this way it doesnt stay with the mob data

				ResourceLocation mobLocation = new ResourceLocation(namespace, path);
				
				if (ForgeRegistries.ENTITY_TYPES.containsKey(mobLocation)) {
					var mobType = ForgeRegistries.ENTITY_TYPES.getDelegate(mobLocation).get().get();
					// spawn the entity if the entity type is not null (we use allData for our mob since it should only have the mob data now)
					if (mobType != null) {
						// level, tags, component, player, click pos, spawn type, bool, bool
						mobType.spawn((ServerLevel) context.getLevel(), allData, null, context.getPlayer(), new BlockPos(clickPos), MobSpawnType.SPAWN_EGG, false, false);
						
						// spawn particles at click location
						((ServerLevel) context.getLevel()).sendParticles(ParticleTypes.CLOUD, clickPos.x, clickPos.y + (mobType.getHeight() / 2), clickPos.z, 25, 0, 0, 0, .05);	// random (increase is also amount of particles), x, y, z, speed	
					}
				}
			}
		}
		
		return InteractionResult.PASS;
	}



	// a function that allows the user to pick up living entities
	public static void pickUpEntity(ItemStack stack, LivingEntity entity) {
		
		// get the tags in order to make sure that the item doesnt already have an entity in it
		CompoundTag itemData = stack.getTag();
		
		if (itemData == null) {
			itemData = new CompoundTag();
		}
		
		var tag = itemData.get(HELD_ENTITY_KEY);
		
		if (!entity.isAlive() || (tag != null && !tag.getAsString().isBlank())) { // makes sure the player doesnt try to pick up a mob or entity that is not living
			return; // this prevents this function from being called on the server if another entity is already inside of the item
		}

		if (entity.getLevel().getClass() == ServerLevel.class) {

			// Item accurately finds entities
			CaptureCraft.LOGGER.info("Name: " + entity.getName().getString());			
			
			CaptureCraft.LOGGER.info("Health: " + String.valueOf(entity.getHealth())); // health is counted for as well
			CaptureCraft.LOGGER.info("Armor: " + String.valueOf(entity.getArmorValue())); // as well as armor

			// spawn particles at entity location
			((ServerLevel) entity.getLevel()).sendParticles(ParticleTypes.CLOUD, entity.xo, entity.yo + (entity.getBbHeight() / 2), entity.zo, 25, 0, 0, 0, .05);	// random (increase is also amount of particles), x, y, z, speed	
			

			// and delete it from the world
			entity.remove(RemovalReason.UNLOADED_WITH_PLAYER);
			
			// merges the item data with the end data, if the item has any data in the first place
			if (!itemData.isEmpty()) {
				String itemKeys = "";
				
				for (String key : itemData.getAllKeys()) {
					
					if (!itemKeys.isBlank()) {
						itemKeys += ", ";
					}
					
					itemKeys += key;	
				}
				
				itemData.putString(ITEM_DATA_KEY, itemKeys);
			}
			
			// merges the entity data with the end data
			CompoundTag entityData = entity.serializeNBT();
			itemData.merge(entityData);
			
			itemData.putString(HELD_ENTITY_KEY, entity.getEncodeId()); // add entity type into the stack
			
			CaptureCraft.LOGGER.info(entity.getEncodeId()); // this gets both the mod and the mob type

			stack.setTag(itemData);
			
			CaptureCraft.LOGGER.info("Keys: " + stack.getTag().getAllKeys().toString());
		}
	}
	
	// Subscribe top the player event for interacting with living entities and see if tags will save there
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void EntityInteract(PlayerInteractEvent.EntityInteract event) {
		var stack = event.getItemStack();
		
		if (stack.getItem() instanceof MainItem && event.getTarget() instanceof LivingEntity){
			
			pickUpEntity(event.getItemStack(), (LivingEntity) event.getTarget()); // put the entity into the tags of the itemstack
			
			if (event.hasResult()) {
				event.setResult(Result.ALLOW);
			}
		}
	}
	
	// override hovering text
	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
	    String message = "Click a mob to pick them up.";
		
	    var tag = stack.getTag();
	    
	    if (tag != null && tag.contains("CustomName")) {
	    	String customName = tag.getString("CustomName");
	    	
	    	customName = customName.substring("{\"text\":\"".length(), customName.length() - 2);
	    	
	    	message = "Currently Holding: " + customName;
	    }
	    else if (tag != null && tag.contains(HELD_ENTITY_KEY) && !tag.get(HELD_ENTITY_KEY).getAsString().isBlank()) {
	    	String[] entityLoc = tag.get(HELD_ENTITY_KEY).getAsString().split(":");
			
			String namespace = entityLoc[0];
			String path = entityLoc[1];
			
			CaptureCraft.LOGGER.info(tag.getAsString());
			CaptureCraft.LOGGER.info(namespace);
			CaptureCraft.LOGGER.info(path);

			ResourceLocation mobLocation = new ResourceLocation(namespace, path);
			
			if (ForgeRegistries.ENTITY_TYPES.containsKey(mobLocation)) {
				var mobType = ForgeRegistries.ENTITY_TYPES.getDelegate(mobLocation).get().get();

				message = "Currently Holding: " + mobType.getDescription().getString();
			}
	    }
	    
		tooltip.add(Component.literal(message));

	    super.appendHoverText(stack, worldIn, tooltip, flagIn);
	}

	// this just makes the item look like its enchanted when it has an entity in it
	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(HELD_ENTITY_KEY) && !stack.getTag().get(HELD_ENTITY_KEY).getAsString().isBlank();
	}
}
