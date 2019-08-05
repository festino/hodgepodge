package com.festp.dispenser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftAgeable;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftAnimals;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.festp.Main;
import com.festp.utils.Utils;
import com.festp.utils.UtilsType;
import com.festp.utils.Vector3i;

import net.minecraft.server.v1_14_R1.EntityAgeable;
import net.minecraft.server.v1_14_R1.EntityAnimal;

public class DropActions implements Listener {
	Main pl;

	int max_dy = 50;
	int max_dxz = 50; // TO DO: configurable pump area
	int min_dxz = -max_dxz;
	int pump_area = max_dxz*2 + 1;
	
	static Enchantment bottomless_bucket_metaench = Enchantment.ARROW_INFINITE;
	
	//cauldrons to fill with dispenser
	List<ArrayList<Integer>> disps = new ArrayList<ArrayList<Integer>>();
	List<BlockState> disp_caul = new ArrayList<>();
	//dispensers to feed animals
	List<BlockState> dbf_disp = new ArrayList<>();
	List<Block> dbf_block = new ArrayList<>();
	List<Material> dbf_food = new ArrayList<>();
	List<Integer[]> dbf_food_slots = new ArrayList<>();
	List<EntityAnimal> loveanimals = new ArrayList<>();
	//dispensers to pump the water
	List<Dispenser> disps_pump = new ArrayList<>();

	enum PumpReadiness {READY, MODULE, NONE};
	enum PumpType {NONE, REGULAR, ADVANCED};
	
	public DropActions(Main plugin) {
		this.pl = plugin;
	}
	
	public void onTick() {
		//dispbreed
		for(int i=0;i<dbf_disp.size();i++) {
			dispenserAnimals(dbf_disp.get(i),dbf_block.get(i),dbf_food.get(i),dbf_food_slots.get(i));
		}
		dbf_disp = new ArrayList<>();
		dbf_block = new ArrayList<>();
		dbf_food = new ArrayList<>();
		dbf_food_slots = new ArrayList<>();
		//lovehearths
		for(int i=0;i<loveanimals.size();i++) {
			/*int love = (Integer)getPrivateField("bx", EntityAnimal.class, loveanimals.get(i));
			if(love > 0)
			{
				//loveanimals.get(i).n();
				//setPrivateField("bx", EntityAnimal.class, loveanimals.get(i), love);
				if(love%10 == 0) summonHearths(loveanimals.get(i));
			}
			else {*/
				loveanimals.remove(i);
				i--;
			/*}*/
		}
		//dispwater
		for(int i=0;i<disp_caul.size();i+=2) {
			int o = 0;
            Inventory inv = ((Dispenser)disp_caul.get(i)).getInventory();
			for(int slot=0;slot<9;slot++)
				if(o<disps.get(i/2).size() && slot != disps.get(i/2).get(o) || o>=disps.get(i/2).size()) {
					if(inv.getItem(slot) != null && inv.getItem(slot).getType() == Material.WATER_BUCKET) {
	                	inv.setItem(slot,new ItemStack(Material.BUCKET));
	                    Utils.full_cauldron_water(disp_caul.get(i+1));
		                break;
					}
				} else o++;
		}
		disp_caul = new ArrayList<>();
		disps = new ArrayList<ArrayList<Integer>>();
		
		for(int i=disps_pump.size()-1;i>=0;i--) {
			dispenserPump(disps_pump.get(i));
			disps_pump.remove(i);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onBlockDispense(BlockDispenseEvent event)
	{
		if(event.getItem() != null && event.getBlock().getType() == Material.DISPENSER)
		{
            BlockState dispenser = event.getBlock().getState(); //0 - down, 1 - up, 2 - north(-z), 3 - south(+z), 4 - west(-x), 5 - east(+x) 
			byte data = (byte) (dispenser.getData().getData()%8);
			int x2 = (data==4 ? -1 : (data==5 ? 1 : 0)), y2 = (data==0 ? -1 : (data==1 ? 1 : 0)), z2 = (data==2 ? -1 : (data==3 ? 1 : 0));
			int x = event.getBlock().getX(), y = event.getBlock().getY(), z = event.getBlock().getZ();
			final Block block = event.getBlock().getWorld().getBlockAt(x+x2, y+y2, z+z2);
			boolean breed = false;
			
			
			
			if(event.getItem().getType().equals(Material.WATER_BUCKET))
			{
				if(block.getType() == Material.CAULDRON)
				{
	                BlockState cauldron = block.getState();
	                if(cauldron.getData().getData() < 3) {
	                    event.setCancelled(true);
	                    //int i = disp_caul.size()/2;
	                    disp_caul.add(dispenser);
	                    disp_caul.add(cauldron);
	                    Inventory inv = ((Dispenser)dispenser).getInventory();
	                    ArrayList<Integer> e = new ArrayList<>();
						for(int slot=0;slot<9;slot++)
							if(inv.getItem(slot) != null && inv.getItem(slot).getType() == Material.WATER_BUCKET)
								e.add(slot);
						disps.add(e);
	                }
	                return;
				}
			}
			
			
			
			else if(event.getItem().getType().equals(Material.WHEAT))
			{
				for(Entity e : event.getBlock().getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1))
						if((e.getType() == EntityType.COW || e.getType() == EntityType.SHEEP) && ((CraftAnimals) e).isAdult()
								&& !(((CraftAnimals) e).getHandle()).isInLove() && !((Integer)( Utils.getPrivateField("b", EntityAgeable.class, (((CraftAnimals) e).getHandle())) ) > 0))
						{
							breed = true;
							break;
						} else if((e.getType() == EntityType.COW || e.getType() == EntityType.SHEEP) && !((CraftAnimals) e).isAdult()) {
							breed = true;
							break;
						}
			}
			else if(event.getItem().getType().equals(Material.CARROT) || event.getItem().getType().equals(Material.POTATO) || event.getItem().getType().equals(Material.BEETROOT))
			{
				for(Entity e : event.getBlock().getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1))
						if(e.getType() == EntityType.PIG && ((CraftAnimals) e).isAdult()
						&& !(((CraftAnimals) e).getHandle()).isInLove() && !((Integer)( Utils.getPrivateField("b", EntityAgeable.class, (((CraftAnimals) e).getHandle())) ) > 0))
						{
							breed = true;
							break;
						} else if(e.getType() == EntityType.PIG && !((CraftAnimals) e).isAdult()) {
							breed = true;
							break;
						}
			}
			else if(event.getItem().getType().equals(Material.WHEAT_SEEDS) || event.getItem().getType().equals(Material.MELON_SEEDS) || event.getItem().getType().equals(Material.PUMPKIN_SEEDS) || event.getItem().getType().equals(Material.BEETROOT_SEEDS))
			{
				for(Entity e : event.getBlock().getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1))
						if(e.getType() == EntityType.CHICKEN && ((CraftAnimals) e).isAdult()
						&& !(((CraftAnimals) e).getHandle()).isInLove() && !((Integer)( Utils.getPrivateField("b", EntityAgeable.class, (((CraftAnimals) e).getHandle())) ) > 0))
						{
							breed = true;
							break;
						} else if(e.getType() == EntityType.CHICKEN && !((CraftAnimals) e).isAdult()) {
							breed = true;
							break;
						}
			}
			
			if(breed)
			{
				dbf_disp.add(dispenser);
				dbf_block.add(block);
				dbf_food.add(event.getItem().getType());
				Inventory inv = ((Dispenser)dispenser).getInventory();
				dbf_food_slots.add(new Integer[] {
						inv.getItem(0) == null ? 0 : inv.getItem(0).getAmount(),
						inv.getItem(1) == null ? 0 : inv.getItem(1).getAmount(),
						inv.getItem(2) == null ? 0 : inv.getItem(2).getAmount(),
						inv.getItem(3) == null ? 0 : inv.getItem(3).getAmount(), 
						inv.getItem(4) == null ? 0 : inv.getItem(4).getAmount(),
						inv.getItem(5) == null ? 0 : inv.getItem(5).getAmount(),
						inv.getItem(6) == null ? 0 : inv.getItem(6).getAmount(),
						inv.getItem(7) == null ? 0 : inv.getItem(7).getAmount(),
						inv.getItem(8) == null ? 0 : inv.getItem(8).getAmount() });
				event.setCancelled(true);
			}
			
			
			
			else {
				Dispenser d = ((Dispenser)dispenser);
				PumpReadiness pr = dispenserPump_test(d, event.getItem());
				if(pr == PumpReadiness.READY) {
					disps_pump.add(d);
					event.setCancelled(true);
				}
				else if(pr == PumpReadiness.MODULE) {
					event.setCancelled(true);
				}
			}
		}
	}
	
	public void dispenserCauldron() {
		
	}
	
	public void dispenserAnimals(BlockState d, Block b, Material f, Integer[] slots) {
		//dispencer test and handle slot
		Inventory inv = ((Dispenser)d).getInventory();
		Integer it = null;
		for(int slot=0;slot<9;slot++)
			if(inv.getItem(slot) != null && inv.getItem(slot).getType() == f && inv.getItem(slot).getAmount() != slots[slot]) {
				it = slot;
			}
		if(it == null) return;
		
		//Breeding
		boolean animalFound = false;
		EntityAnimal loveanimal = null;
		if(f.equals(Material.WHEAT))
		{
			for(Entity e : b.getWorld().getNearbyEntities(b.getLocation(), 1, 1, 1))
					if((e.getType() == EntityType.COW || e.getType() == EntityType.SHEEP) && ((CraftAnimals) e).isAdult()
							&& !(((CraftAnimals) e).getHandle()).isInLove() && !((Integer)( Utils.getPrivateField("b", EntityAgeable.class, (((CraftAnimals) e).getHandle())) ) > 0))
					{
						//(((CraftAnimals) e).getHandle()).f(exp_hop);
						animalFound = Utils.setLove(((CraftAnimals) e).getHandle(), f);
						if(animalFound) loveanimal = ((CraftAnimals) e).getHandle();
						break;
					} else if((e.getType() == EntityType.COW || e.getType() == EntityType.SHEEP) && !((CraftAnimals) e).isAdult()) {
						((CraftAgeable) e).getHandle().setAge((int)(-((CraftAgeable) e).getAge() / 20 * 0.1F), true);
						animalFound = true;
						break;
					}
		}
		else if(f.equals(Material.CARROT) || f.equals(Material.POTATO) || f.equals(Material.BEETROOT))
		{
			for(Entity e : b.getWorld().getNearbyEntities(b.getLocation(), 1, 1, 1))
					if(e.getType() == EntityType.PIG && ((CraftAnimals) e).isAdult()
					&& !(((CraftAnimals) e).getHandle()).isInLove() && !((Integer)( Utils.getPrivateField("b", EntityAgeable.class, (((CraftAnimals) e).getHandle())) ) > 0))
					{
						animalFound = Utils.setLove(((CraftAnimals) e).getHandle(), f);
						if(animalFound) loveanimal = ((CraftAnimals) e).getHandle();
						break;
					} else if(e.getType() == EntityType.PIG && !((CraftAnimals) e).isAdult()) {
						((CraftAgeable) e).getHandle().setAge((int)(-((CraftAgeable) e).getAge() / 20 * 0.1F), true);
						animalFound = true;
						break;
					}
		}
		else if(f.equals(Material.WHEAT_SEEDS) || f.equals(Material.MELON_SEEDS) || f.equals(Material.PUMPKIN_SEEDS) || f.equals(Material.BEETROOT_SEEDS))
		{
			for(Entity e : b.getWorld().getNearbyEntities(b.getLocation(), 1, 1, 1))
					if(e.getType() == EntityType.CHICKEN && ((CraftAnimals) e).isAdult()
					&& !(((CraftAnimals) e).getHandle()).isInLove() && !((Integer)( Utils.getPrivateField("b", EntityAgeable.class, (((CraftAnimals) e).getHandle())) ) > 0))
					{
						animalFound = Utils.setLove(((CraftAnimals) e).getHandle(), f);
						if(animalFound) loveanimal = ((CraftAnimals) e).getHandle();
						break;
					} else if(e.getType() == EntityType.CHICKEN && !((CraftAnimals) e).isAdult()) {
						((CraftAgeable) e).getHandle().setAge((int)(-((CraftAgeable) e).getAge() / 20 * 0.1F), true);
						animalFound = true;
						break;
					}
		}
		
		if(animalFound) {
			inv.setItem(it, inv.getItem(it).getAmount() > 1 ? new ItemStack(f, inv.getItem(it).getAmount()-1) : new ItemStack(Material.AIR));
			loveanimals.add(loveanimal);
		}
	}
	
	public PumpReadiness dispenserPump_test(Dispenser d, ItemStack dropped) {
		Inventory inv = d.getInventory();
		//test empty bucket
		//test pump module??? - it had already worked
		int bucket_index = -2, module_index = -2, multybucket_index = -2, null_index = -1, pipe_index = -1;
		ItemStack is;
		for(int i = -1; i < 9; i++) {
			if(i<0) is = dropped;
			else is = inv.getItem(i);
			if(is != null)
			{
				if(module_index < -1 && is.getType() == Material.BLAZE_ROD
						&& is.hasItemMeta() && is.getItemMeta().hasLore()) {
					String lore = is.getItemMeta().getLore().get(0).toLowerCase(Locale.ENGLISH);
					if(lore.contains("pump") || lore.contains("����") ) {
						module_index = i;
						if(bucket_index >= -1 || (multybucket_index >= -1 && null_index >= 0)) break;
					}
				}
				else if( is.getType() == Material.BUCKET ) {
					if(is.getEnchantmentLevel(bottomless_bucket_metaench) > 0)
						bucket_index = 9;
					else if(is.getAmount() == 1 && bucket_index < -1)
						bucket_index = i;
					else if( multybucket_index < -1) {
						multybucket_index = i;
						if(null_index < 0) continue;
					}
					if(module_index >= -1) break;
				}
				else if(is.getType() == Material.NETHER_BRICK_FENCE)
					pipe_index = i;
			}
			else if(null_index < 0) null_index = i;
		}
		//System.out.println("TEST: "+ module_index+" "+bucket_index+" "+multybucket_index+" "+null_index);
		if(module_index >= -1) {
			if(bucket_index >= -1 || (multybucket_index >= -1 && null_index >= 0) || pipe_index >= -1) {
				return PumpReadiness.READY;
			}
			return PumpReadiness.MODULE;
		}
		return PumpReadiness.NONE;
	}
	
	public void dispenserPump(Dispenser d) {
		Inventory inv = d.getInventory();
		PumpType pump_type = PumpType.NONE;
		int bucket_index = -1, module_index = -1;
		int pipe_index = -1, null_index = -1, multybucket_index = -1;
		for(int i = 0; i < 9; i++) {
			ItemStack is;
			is = inv.getItem(i);
			if(is != null)
			{
				if(module_index < 0 && is.getType() == Material.BLAZE_ROD
						&& is.hasItemMeta() && is.getItemMeta().hasLore()) {
					String lore = is.getItemMeta().getLore().get(0).toLowerCase(Locale.ENGLISH); //new Locale("ru")
					if(lore.contains("pump") || lore.contains("����") ) {
						if(lore.contains("regular") || lore.contains("�����"))
						{
							module_index = i;
							pump_type = PumpType.REGULAR;
							if(bucket_index >= 0) break;
						}
						else if(lore.contains("advanced") || lore.contains("���������"))
						{
							module_index = i;
							pump_type = PumpType.ADVANCED;
							if(bucket_index >= 0) break;
						}
					}
				}
				else if( is.getType() == Material.BUCKET ) {
					if(is.getEnchantmentLevel(bottomless_bucket_metaench) > 0)
						bucket_index = 9;
					else if(is.getAmount() == 1 && bucket_index < 0)
						bucket_index = i;
					else {
						if( multybucket_index < 0)
							multybucket_index = i;
						if(null_index < 0) continue;
					}
					if(module_index >= 0) break;
				}
				else if(is.getType() == Material.NETHER_BRICK_FENCE)
					pipe_index = i;
			}
			else if(null_index < 0) null_index = i;
		}
		//System.out.println("WORK: "+ module_index+" "+bucket_index+" "+multybucket_index+" "+null_index+"   "+pipe_index );
		if(module_index >= 0 && ( bucket_index >= 0 || pipe_index >= 0 || (null_index >= 0 && multybucket_index >= 0)) ) {
			if(pump_type == PumpType.REGULAR)
				work_regularPump(d, bucket_index, null_index, multybucket_index);
			else if(pump_type == PumpType.ADVANCED)
				work_advancedPump(d, bucket_index, null_index, multybucket_index);
		}
	}
	
	public void work_regularPump(Dispenser d, int bucket_index, int null_index, int multybucket_index) {
		Inventory inv = d.getInventory();
		byte data = (byte) (d.getData().getData()%8);
		int x2 = (data==4 ? -1 : (data==5 ? 1 : 0)), y2 = (data==0 ? -1 : (data==1 ? 1 : 0)), z2 = (data==2 ? -1 : (data==3 ? 1 : 0));
		/*int x = d.getBlock().getX(), y = d.getBlock().getY(), z = d.getBlock().getZ();
		final Block block = d.getBlock().getWorld().getBlockAt(x+x2, y+y2, z+z2);*/
		Block block = d.getBlock().getRelative(x2, y2, z2);
		Block block_to_pump = null;
		//can place pipe
		if (y2 <= 0) {
			int pipes = 0;
			//scroll all placed pipe blocks
			while (block.getType() == Material.NETHER_BRICK_FENCE) {
				block = block.getRelative(0, -1, 0);
				pipes += 1;
			}
			int pipe_index = -1;
			Block test_liquid = block;
			//test available liquid below pipe
			while (UtilsType.isAir(test_liquid.getType()) || UtilsType.isFlowingLiquid(test_liquid) || test_liquid.getType() == Material.NETHER_BRICK_FENCE
					 || UtilsType.isSlab(test_liquid.getType())) {
				if (test_liquid.isLiquid()) {
					block_to_pump = findBlockToPump_regular(test_liquid);
					if (block_to_pump == null)
						test_liquid = test_liquid.getRelative(0, -1, 0);
					else break;
				}
				test_liquid = test_liquid.getRelative(0, -1, 0);
			}
			//remove fences
			if (findBlockToPump_regular(test_liquid) == null) {
				if (pipes > 0) {
					for (int i = 0; i < 9; i++) {
						ItemStack is;
						is = inv.getItem(i);
						if (is != null && is.getType() == Material.NETHER_BRICK_FENCE && is.getAmount() < 64)
						{
							pipe_index = i;
							is.setAmount(is.getAmount()+1);
							block.getRelative(0, 1, 0).setType(Material.AIR);
							break;
						}
					}
					if (pipe_index < 0 && null_index >= 0) {
						inv.setItem(null_index, new ItemStack(Material.NETHER_BRICK_FENCE, 1));
						block.getRelative(0, 1, 0).setType(Material.AIR);
					}
				}
				return;
			}
			//place fences
			block_to_pump = findBlockToPump_regular(block);
			if (block.isEmpty() || (block.isLiquid() && block_to_pump == null)) {
				for (int i = 0; i < 9; i++) {
					ItemStack is;
					is = inv.getItem(i);
					if(is != null && is.getType() == Material.NETHER_BRICK_FENCE)
					{
						pipe_index = i;
						break;
					}
				}
				if (pipe_index >= 0) {
					ItemStack pipe = inv.getItem(pipe_index);
					pipe.setAmount(pipe.getAmount() - 1);
					block.setType(Material.NETHER_BRICK_FENCE);
					block = block.getRelative(0, -1, 0);
				}
				return;
			}
		}
		if ( bucket_index < 0 && ( multybucket_index < 0 || null_index < 0 ) )
			return;
		
		//pump
		if (block_to_pump == null)
			block_to_pump = findBlockToPump_regular(block);
		if (block_to_pump != null) {
			if (bucket_index < 9) {
				if (bucket_index < 0) {
					inv.getItem(multybucket_index).setAmount(inv.getItem(multybucket_index).getAmount()-1);
					bucket_index = null_index;
				}
				if (block_to_pump.getType() == Material.LAVA)
					inv.setItem(bucket_index, new ItemStack(Material.LAVA_BUCKET));
				else if (block_to_pump.getType() == Material.WATER)
					inv.setItem(bucket_index, new ItemStack(Material.WATER_BUCKET));
			}
			block_to_pump.setType(Material.AIR);
		}
	}
	
	public void work_advancedPump(Dispenser d, int bucket_index, int null_index, int multybucket_index) {
		//  _.._
		// |    |
		// |____|
		//  ____
		// |    |
		// |    =
		// |____|
		//  ____
		// |    |
		// |_.._|
		byte data = (byte) (d.getData().getData()%8);
		int x2 = (data==4 ? -1 : (data==5 ? 1 : 0)), y2 = (data==0 ? -1 : (data==1 ? 1 : 0)), z2 = (data==2 ? -1 : (data==3 ? 1 : 0));
		/*int x = d.getBlock().getX(), y = d.getBlock().getY(), z = d.getBlock().getZ();
		final Block block = d.getBlock().getWorld().getBlockAt(x+x2, y+y2, z+z2);*/
		Block block = d.getBlock().getRelative(x2, y2, z2);
		Block block_to_pump = null;
		Inventory inv = d.getInventory();
		if (y2 <= 0)
		{
			int pipes = 0;
			while (block.getType() == Material.NETHER_BRICK_FENCE) {
				block = block.getRelative(0, -1, 0);
				pipes += 1;
			}
			int pipe_index = -1;
			Block test_liquid = block;
			while (UtilsType.isAir(test_liquid.getType()) || test_liquid.getType() == Material.NETHER_BRICK_FENCE)
				test_liquid = test_liquid.getRelative(0, -1, 0);
			
			//remove fences
			block_to_pump = findBlockToPump_advanced(test_liquid);
			if (block_to_pump == null) {
				if (pipes > 0) {
					//find slot to add 1 fence
					for (int i = 0; i < 9; i++) {
						ItemStack is;
						is = inv.getItem(i);
						if (is != null && is.getType() == Material.NETHER_BRICK_FENCE && is.getAmount() < 64)
						{
							pipe_index = i;
							is.setAmount(is.getAmount()+1);
							block.getRelative(0, 1, 0).setType(Material.AIR);
							break;
						}
					}
					if (pipe_index < 0 && null_index >= 0) {
						inv.setItem(null_index, new ItemStack(Material.NETHER_BRICK_FENCE, 1));
						block.getRelative(0, 1, 0).setType(Material.AIR);
					}
				}
				return;
			}
			//place fences
			block_to_pump = findBlockToPump_advanced(block);
			if (block.isEmpty() || (block.isLiquid() && block_to_pump == null)) {
				//find slot to remove 1 fence
				for (int i = 0; i < 9; i++) {
					ItemStack is;
					is = inv.getItem(i);
					if (is != null && is.getType() == Material.NETHER_BRICK_FENCE)
					{
						pipe_index = i;
						break;
					}
				}
				if (pipe_index >= 0) {
					ItemStack pipe = inv.getItem(pipe_index);
					pipe.setAmount(pipe.getAmount() - 1);
					block.setType(Material.NETHER_BRICK_FENCE);
					block = block.getRelative(0, -1, 0);
				}
				return;
			}
		}
		if( bucket_index < 0 && ( multybucket_index < 0 || null_index < 0 ) )
			return;
		
		//pump
		if (block_to_pump == null)
			block_to_pump = findBlockToPump_advanced(block);
		if (block_to_pump != null) {
			if (bucket_index < 9) {
				if (bucket_index < 0) {
					inv.getItem(multybucket_index).setAmount(inv.getItem(multybucket_index).getAmount() - 1);
					bucket_index = null_index;
				}
				if (block_to_pump.getType() == Material.LAVA)
					inv.setItem(bucket_index, new ItemStack(Material.LAVA_BUCKET));
				else if (block_to_pump.getType() == Material.WATER)
					inv.setItem(bucket_index, new ItemStack(Material.WATER_BUCKET));
			}
			block_to_pump.setType(Material.AIR);
		}
	}

	public Block findBlockToPump_advanced(Block block)
	{
		Block max_dist_block = null;
		if(block.isLiquid())
		{
			int top_layer_dy = 0;
			List<LayerSet> layers = new ArrayList<>();
			LayerSet top_layer = new LayerSet(max_dxz);
			layers.add(top_layer);
			
			int dist = 0;
			List<Vector3i> unchecked = new ArrayList<>();
			List<Vector3i> next_unchecked = new ArrayList<>();
			next_unchecked.add(new Vector3i(0, 0, 0));
			while (next_unchecked.size() > 0)
			{
				dist++;
				unchecked = next_unchecked;
				next_unchecked = new ArrayList<>();
				for (Vector3i loc : unchecked)
				{
					int dx = loc.getX(), dy = loc.getY(), dz = loc.getZ();
					Block b = block.getRelative(dx, dy, dz);
					
					if (dy >= layers.size())
					{
						top_layer = new LayerSet(max_dxz);
						layers.add(top_layer);
						top_layer_dy++;
					}
					if (dy == top_layer_dy && UtilsType.isStationaryLiquid(b))
					{
						if (top_layer.farthest[0] == null || dist > top_layer.max_distance)
						{
							top_layer.farthest[0] = dx;
							top_layer.farthest[1] = dz;
							top_layer.max_distance = dist;
						}
					}
					
					layers.get(dy).setDistance(dx, dz, dist);
					
					Block rel;
					if (dy < max_dy) {
						rel = b.getRelative(0, 1, 0); //BlockFace.UP
						if (rel.isLiquid())
							if (dy >= top_layer_dy)
								next_unchecked.add(new Vector3i(dx, dy + 1, dz));
							else if (layers.get(dy + 1).isUnchecked(dx, dz)) {
								next_unchecked.add(new Vector3i(dx, dy + 1, dz));
								layers.get(dy + 1).setNext(dx, dz);
							}
					}
					if (dx < max_dxz) {
						rel = b.getRelative(1, 0, 0);
						if (rel.isLiquid())
							if (layers.get(dy).isUnchecked(dx + 1, dz)) {
								next_unchecked.add(new Vector3i(dx + 1, dy, dz));
								layers.get(dy).setNext(dx + 1, dz);
							}
					}
					if (min_dxz < dx) {
						rel = b.getRelative(-1, 0, 0);
						if (rel.isLiquid())
							if (layers.get(dy).isUnchecked(dx - 1, dz)) {
								next_unchecked.add(new Vector3i(dx - 1, dy, dz));
								layers.get(dy).setNext(dx - 1, dz);
							}
					}
					if (dz < max_dxz) {
						rel = b.getRelative(0, 0, 1);
						if (rel.isLiquid())
							if (layers.get(dy).isUnchecked(dx, dz + 1)) {
								next_unchecked.add(new Vector3i(dx, dy, dz + 1));
								layers.get(dy).setNext(dx, dz + 1);
							}
					}
					if (min_dxz < dz) {
						rel = b.getRelative(0, 0, -1);
						if (rel.isLiquid())
							if (layers.get(dy).isUnchecked(dx, dz - 1)) {
								next_unchecked.add(new Vector3i(dx, dy, dz - 1));
								layers.get(dy).setNext(dx, dz - 1);
							}
					}
				}
			}
			
			for (int dy = layers.size() - 1; dy >= 0; dy--) {
				LayerSet layer = layers.get(dy);
				if (layer.isDefinedFarthest())
				{
					int dx = layer.farthest[0];
					int dz = layer.farthest[1];
					max_dist_block = block.getRelative(dx, dy, dz);
					break;
				}
			}
			/*for (dy = 0; dy < layers.size(); dy++)
			{
				LayerSet layer = layers.get(dy);
				layer.print_scale(block.getY() + dy);
			}*/
		}
		return max_dist_block;
	}
	

	public Block findBlockToPump_regular(Block block)
	{
		int max_distance = 0;
		Block max_dist_block = null;
		if(!block.isLiquid()) return null;
		if(UtilsType.isStationaryLiquid(block.getRelative(2, 0, 0)) && block.getRelative(1, 0, 0).isLiquid()) {
			max_dist_block = block.getRelative(2, 0, 0);
			max_distance = 3;
		} else if(max_distance < 1 && UtilsType.isStationaryLiquid(block.getRelative(1, 0, 0))) {
			max_dist_block = block.getRelative(1, 0, 0);
			max_distance = 1;
		}
		if(UtilsType.isStationaryLiquid(block.getRelative(0, 0, 2)) && block.getRelative(0, 0, 1).isLiquid()) {
			max_dist_block = block.getRelative(0, 0, 2);
			max_distance = 3;
		} else if(max_distance < 1 && UtilsType.isStationaryLiquid(block.getRelative(0, 0, 1))) {
			max_dist_block = block.getRelative(0, 0, 1);
			max_distance = 1;
		}
		if(UtilsType.isStationaryLiquid(block.getRelative(-2, 0, 0)) && block.getRelative(-1, 0, 0).isLiquid()) {
			max_dist_block = block.getRelative(-2, 0, 0);
			max_distance = 3;
		} else if(max_distance < 1 && UtilsType.isStationaryLiquid(block.getRelative(-1, 0, 0))) {
			max_dist_block = block.getRelative(-1, 0, 0);
			max_distance = 1;
		}
		if(UtilsType.isStationaryLiquid(block.getRelative(0, 0, -2)) && block.getRelative(0, 0, -1).isLiquid()) {
			max_dist_block = block.getRelative(0, 0, -2);
			max_distance = 3;
		} else if(max_distance < 1 && UtilsType.isStationaryLiquid(block.getRelative(0, 0, -1))) {
			max_dist_block = block.getRelative(0, 0, -1);
			max_distance = 1;
		}
		if(max_distance < 2 && UtilsType.isStationaryLiquid(block.getRelative(1, 0, 1)) && (block.getRelative(1, 0, 0).isLiquid() || block.getRelative(0, 0, 1).isLiquid()))
			max_dist_block = block.getRelative(1, 0, 1);
		else if(max_distance < 2 && UtilsType.isStationaryLiquid(block.getRelative(-1, 0, 1)) && (block.getRelative(-1, 0, 0).isLiquid() || block.getRelative(0, 0, 1).isLiquid())) 
			max_dist_block = block.getRelative(-1, 0, 1);
		else if(max_distance < 2 && UtilsType.isStationaryLiquid(block.getRelative(-1, 0, -1)) && (block.getRelative(-1, 0, 0).isLiquid() || block.getRelative(0, 0, -1).isLiquid())) 
			max_dist_block = block.getRelative(-1, 0, -1);
		else if(max_distance < 2 && UtilsType.isStationaryLiquid(block.getRelative(1, 0, -1)) && (block.getRelative(1, 0, 0).isLiquid() || block.getRelative(0, 0, -1).isLiquid())) 
			max_dist_block = block.getRelative(1, 0, -1);
		if(max_distance == 0 && UtilsType.isStationaryLiquid(block)) {
			max_dist_block = block;
		}
		return max_dist_block;
	}
}
