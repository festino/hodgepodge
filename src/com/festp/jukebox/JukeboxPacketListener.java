package com.festp.jukebox;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Jukebox;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.PacketPlayOutEntity.PacketPlayOutRelEntityMove;
import net.minecraft.server.v1_16_R2.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_16_R2.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_16_R2.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_16_R2.PacketPlayOutKeepAlive;
import net.minecraft.server.v1_16_R2.PacketPlayOutLightUpdate;
import net.minecraft.server.v1_16_R2.PacketPlayOutMap;
import net.minecraft.server.v1_16_R2.PacketPlayOutMapChunk;
import net.minecraft.server.v1_16_R2.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_16_R2.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_16_R2.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.server.v1_16_R2.PacketPlayOutScoreboardObjective;
import net.minecraft.server.v1_16_R2.PacketPlayOutScoreboardScore;
import net.minecraft.server.v1_16_R2.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_16_R2.PacketPlayOutSpawnPosition;
import net.minecraft.server.v1_16_R2.PacketPlayOutStopSound;
import net.minecraft.server.v1_16_R2.PacketPlayOutUpdateTime;
import net.minecraft.server.v1_16_R2.PacketPlayOutWorldEvent;

public class JukeboxPacketListener implements Listener {
	final JukeboxHandler handler;
	
	public JukeboxPacketListener(JukeboxHandler handler) {
		this.handler = handler;
	}
	
	@EventHandler
    public void onJoin(PlayerJoinEvent event){
        injectPlayer(event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        removePlayer(event.getPlayer());
    }
    private void removePlayer(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        channel.eventLoop().submit(() -> {
            channel.pipeline().remove(player.getName());
            return null;
        });
    }

    private void injectPlayer(Player player) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {

            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) throws Exception {
                //Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "PACKET READ: " + ChatColor.RED + packet.toString());
                super.channelRead(channelHandlerContext, packet);
            }

            @Override
            public void write(ChannelHandlerContext channelHandlerContext, Object packet, ChannelPromise channelPromise) throws Exception {


            	//PacketPlayOutAnimation
            	//PacketPlayOutBlockChange
            	//PacketPlayOutWorldEvent
                /*if (!(packet instanceof PacketPlayOutEntityMetadata
                		|| packet instanceof PacketPlayOutEntityVelocity
                		|| packet instanceof PacketPlayOutMap
                		|| packet instanceof PacketPlayOutEntityTeleport
                		|| packet instanceof PacketPlayOutRelEntityMove
                		|| packet instanceof PacketPlayOutPlayerInfo
                		|| packet instanceof PacketPlayOutScoreboardScore
                		|| packet instanceof PacketPlayOutScoreboardObjective
                		|| packet instanceof PacketPlayOutScoreboardDisplayObjective
                		|| packet instanceof PacketPlayOutUpdateTime
                		|| packet instanceof PacketPlayOutSpawnEntity
                		|| packet instanceof PacketPlayOutSpawnPosition
                		|| packet instanceof PacketPlayOutKeepAlive
                		|| packet instanceof PacketPlayOutLightUpdate
                		|| packet instanceof PacketPlayOutMapChunk))
                    Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "PACKET WRITE: " + ChatColor.GREEN + packet.toString());*/
                /*if(packet instanceof PacketPlayOutStopSound){
                	PacketPlayOutStopSound packetPlayOutStopSound = (PacketPlayOutStopSound) packet;
                    Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "PACKET BLOCKED: " + ChatColor.GREEN + packetPlayOutStopSound.toString());
                    return;
                }*/
                /*if(packet instanceof PacketPlayOutNamedSoundEffect){
                	PacketPlayOutNamedSoundEffect packetPlayOutStopSound = (PacketPlayOutNamedSoundEffect) packet;
                	
                    Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "PACKET BLOCKED: " + ChatColor.GREEN + packetPlayOutStopSound.toString());
                    return;
                }*/
                if (packet instanceof PacketPlayOutWorldEvent) {
                	Object obj = getField(packet, "b");
                	if (!(obj instanceof BlockPosition)) {
                		// vanilla obfuscation has changed
                	}
                	BlockPosition pos = (BlockPosition) obj;
                	int x = pos.getX();
                	int y = pos.getY();
                	int z = pos.getZ();
                	// world?
                	for (Jukebox jukebox : handler.getClickedJukeboxes()) {
                		if (jukebox.getX() == x && jukebox.getY() == y && jukebox.getZ() == z) {
                        	//Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "PACKET BLOCKED: " + ChatColor.GREEN + packetPlayOutWorldEvent.toString());
                        	return;
                		}
                	}
                	//Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "PACKET PASSED: " + ChatColor.GREEN + packetPlayOutWorldEvent.toString());
                }
                super.write(channelHandlerContext, packet, channelPromise);
            }


        };

        ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.pipeline();
        pipeline.addBefore("packet_handler", player.getName(), channelDuplexHandler);

    }

    
    // https://github.com/frengor/PacketInjectorAPI/blob/master/src/main/java/com/fren_gor/packetInjectorAPI/ReflectionUtil.java
	private static final Map<String, Map<String, Field>> fields = new ConcurrentHashMap<>();
	
	public static Object getField(Object object, String field) {
		return getField(object, object.getClass(), field);
	}
	private static Object getField(Object object, Class<?> c, String field) {

		if (fields.containsKey(c.getCanonicalName())) {
			Map<String, Field> fs = fields.get(c.getCanonicalName());
			if (fs.containsKey(field)) {
				try {
					return fs.get(field).get(object);
				} catch (ReflectiveOperationException e) {
					return null;
				}
			}
		}

		Class<?> current = c;
		Field f;
		while (true)
			try {
				f = current.getDeclaredField(field);
				break;
			} catch (ReflectiveOperationException e1) {
				current = current.getSuperclass();
				if (current != null) {
					continue;
				}
				return null;
			}

		f.setAccessible(true);

		Map<String, Field> map;
		if (fields.containsKey(c.getCanonicalName())) {
			map = fields.get(c.getCanonicalName());
		} else {
			map = new ConcurrentHashMap<>();
			fields.put(c.getCanonicalName(), map);
		}

		map.put(f.getName(), f);

		try {
			return f.get(object);
		} catch (ReflectiveOperationException e) {
			return null;
		}

	}
}