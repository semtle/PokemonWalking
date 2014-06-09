/**
 * THIS IS CREATED BY tom_mai78101. PLEASE GIVE CREDIT FOR WORKING ON A CLONE.
 * 
 * ALL WORKS COPYRIGHTED TO The Pokémon Company and Nintendo. I REPEAT, THIS IS A CLONE.
 * 
 * YOU MAY NOT SELL COMMERCIALLY, OR YOU WILL BE PROSECUTED BY The Pokémon Company AND Nintendo.
 * 
 * THE CREATOR IS NOT LIABLE FOR ANY DAMAGES DONE. FOLLOW LOCAL LAWS, BE RESPECTFUL, AND HAVE A GOOD DAY!
 * */

package level;

import java.util.ArrayList;

import obstacle.Obstacle;
import screen.BaseScreen;
import abstracts.Tile;
import abstracts.World;
import dialogue.Dialogue;
import dialogue.NewDialogue;
import entity.Player;

public class OverWorld extends World {
	// Overworld properties.
	private boolean invertBitmapColors;
	private int currentAreaSectorID;
	private Dialogue dialogue;
	private NewDialogue newDialogue;
	
	/**
	 * Initializes the overworld in the game.
	 * 
	 * All game entities are to be loaded through this method.
	 * 
	 * @param Player
	 *            Takes a Player object. The overworld then loads all related properties in respect to the Player object.
	 * */
	public OverWorld(Player player, Dialogue dialogue) {
		// There should be a maximum number of areas available for the OverWorld.
		// All areas defined must be placed in WorldConstants.
		this.worldID = WorldConstants.OVERWORLD;
		
		if (!this.areas.isEmpty())
			areas.clear();
		this.areas = WorldConstants.getAllNewAreas();
		
		// Overworld properties
		this.invertBitmapColors = false;
		
		// Player
		this.player = player;
		
		// Going to set this area as test default only. This will need to change in the future.
		this.currentArea = this.areas.get(0);
		this.setCurrentArea(this.areas.get(0));
		this.currentArea.setPlayer(player);
		this.currentArea.setDebugDefaultPosition();
		this.setCurrentAreaSector(0);
		// TODO: Add a method that executes according to the sector ID. Basically,
		// it needs to tell the player that they entered a new sector.
		// Needs a marker in the area that points to where the area connects together.
		
		// Dialogue
		this.dialogue = dialogue;
		this.newDialogue = null;
	}
	
	// Will add this in the future. Currently, the only entity is Player.
	// public void addEntity(Entity e) {
	// //e.initialize(this);
	// //this.tiles.add(e);
	// }
	
	// Worlds no longer need to calculate total width and height.
	// public int getTotalWidth() {
	// int result = 0;
	// for (Area a : areas) {
	// if (a != null)
	// result += a.getWidth();
	// }
	// return result;
	// }
	//
	// public int getTotalHeight() {
	// int result = 0;
	// for (Area a : areas) {
	// if (a != null)
	// result += a.getHeight();
	// }
	// return result;
	// }
	
	// Not sure if these width and height values are useful...
	public int getCurrentAreaWidth() {
		return this.currentArea.getWidth();
	}
	
	public int getCurrentAreaHeight() {
		return this.currentArea.getHeight();
	}
	
	@Override
	public void tick() {
		if (!this.invertBitmapColors)
			this.player.tick();
		// FIXME: Find a way to revert back to default settings when game save data is invalid.
		if (this.currentArea == null)
			this.currentArea = this.areas.get(0);
		this.currentArea.tick();
		
		if (this.currentArea.playerIsInWarpZone() || (this.currentArea.isDisplayingExitArrow() && this.player.isColliding())) {
			this.handleWarpPointEvent();
		}
		
		if (!this.player.isLockedWalking()) {
			if (this.currentArea.getSectorID() != this.currentAreaSectorID) {
				this.currentAreaSectorID = this.currentArea.getSectorID();
				// This is where you get the latest sector id at.
				System.out.println("Area: " + this.currentArea.getAreaID() + " Sector: " + currentArea.getSectorID());
			}
		}
		
		// TODO: Fix the awkward interaction caused by so many states not working properly.
		if (dialogue.isDoneDisplayingDialogue()) {
			dialogue.reset();
			this.player.stopInteraction();
		}
		else if (this.player.isInteracting() && this.player.getInteractionID() != 0) {
			int alpha = (player.getInteractionID() >> 24) & 0xFF;
			int red = (player.getInteractionID() >> 16) & 0xFF;
			switch (alpha) {
				//TODO: Merge "Signs" with "Obstacles", as they now have similar functions.
				case 0x03: {//Obstacles
					ArrayList<Obstacle> list = this.currentArea.getObstaclesList();
					OBSTACLE_LOOP:
						for (int i = 0; i < list.size(); i++){
							Obstacle obstacle = list.get(i);
							if (obstacle.getID() != red)
								continue;
							this.newDialogue = obstacle.getDialogue();
							break OBSTACLE_LOOP;
						}
					break;
				}
				case 0x08: {// Sign
					if (!dialogue.isDisplayingDialogue()) {
						dialogue.createText(alpha, this.player.getInteractionID() & 0xFFFF);
					}
					break;
				}
				case 0x0B: {// Item
					int red_itemType = (player.getInteractionID() >> 16) & 0xFF;
					if (!dialogue.isDisplayingDialogue()) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								PixelData data = currentArea.getCurrentPixelData();
								switch (player.getFacing()) {
									case Player.UP:
										currentArea.setPixelData(data, player.getXInArea(), player.getYInArea() - 1);
										break;
									case Player.DOWN:
										currentArea.setPixelData(data, player.getXInArea(), player.getYInArea() + 1);
										break;
									case Player.LEFT:
										currentArea.setPixelData(data, player.getXInArea() - 1, player.getYInArea());
										break;
									case Player.RIGHT:
										currentArea.setPixelData(data, player.getXInArea() + 1, player.getYInArea());
										break;
								}
							}
						}).start();
						dialogue.createText(alpha, red_itemType);
					}
					break;
				}
			}
			
		}
		
		if (this.newDialogue != null){
			if (this.newDialogue.isDialogueTextSet() && !this.newDialogue.isDialogueCompleted()){
				Player.lockMovements();
				this.newDialogue.tick();
			}
			if (this.newDialogue.isDialogueCompleted()){
				Player.unlockMovements();
				this.newDialogue = null;
			}
		}
		
	}
	
	private void handleWarpPointEvent() {
		boolean currentAreaFound = false;
		int currentAreaID = 0;
		if (WorldConstants.isModsEnabled.booleanValue() && this.currentArea.getAreaID() < 1000)
			currentAreaID = this.currentArea.getAreaID() + 1000;
		else
			currentAreaID = this.currentArea.getAreaID();
		for (int i = 0; i < this.areas.size(); i++) {
			if (this.areas.get(i).getAreaID() == currentAreaID) {
				this.areas.set(i, this.currentArea);
				currentAreaFound = true;
				break;
			}
		}
		if (currentAreaFound) {
			PixelData data = this.currentArea.getCurrentPixelData();
			int targetAreaID = 0;
			if (WorldConstants.isModsEnabled.booleanValue() && this.currentArea.getAreaID() < 1000)
				targetAreaID = data.getTargetAreaID() + 1000;
			else
				targetAreaID = data.getTargetAreaID();
			this.currentArea = WorldConstants.convertToArea(areas, targetAreaID);
			if (currentArea == null) {
				this.currentArea.playerWentPastWarpZone();
				return;
			}
			this.currentArea.setPlayer(this.player);
			this.currentArea.setDefaultPosition(data);
			this.invertBitmapColors = true;
			
			switch ((data.getColor() >> 24) & 0xFF) {
				case 0x04: //Warp point
					this.player.forceLockWalking();
					break;
				case 0x0A: //Door
					this.player.tick();
					break;
				case 0x0C: //Carpet (Indoors)
				case 0x0D: //Carpet (Outdoors)
					this.player.forceLockWalking();
					this.player.tick();
					break;
			}
			
			this.currentArea.playerWentPastWarpZone();
		}
	}
	
	protected void renderTiles(BaseScreen screen, int x0, int y0, int x1, int y1) {
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				this.currentArea.renderTiles(screen, x, y);
			}
		}
	}
	
	@Override
	public void render(BaseScreen screen, int xPlayerPos, int yPlayerPos) {
		// OverWorld offsets are not set.
		// Setting area offsets with player positions
		
		screen.setOffset(screen.getWidth() / 2 - Tile.WIDTH, (screen.getHeight() - Tile.HEIGHT) / 2);
		this.currentArea.renderTiles(screen, xPlayerPos, yPlayerPos);
		screen.setOffset(0, 0);
		
		if (this.invertBitmapColors) {
			if (screen.getRenderingEffectTick() == (byte) 0x7) {
				screen.setRenderingEffectTick((byte) 0x0);
			}
			this.invertBitmapColors = screen.invert();
		}
		
		if (screen.getRenderingEffectTick() < (byte) 0x4 || screen.getRenderingEffectTick() >= (byte) 0x7)
			player.render(screen, 0, 0);
		
		if (this.newDialogue != null)
			this.newDialogue.render(screen, screen.getBufferedImage().createGraphics());
	}
	
	// private void renderTiles(BaseScreen screen, Area area, int xPosition, int yPosition, int xOff, int yOff) {
	// //Unsure at the moment.
	// // area.setPosition(xPosition, yPosition);
	// // area.renderTiles(screen, -xOff, -yOff);
	// }
	
	public Tile getTile(int x, int y) {
		/*
		 * if (pixels[y * this.width + x] != 0xFF00FF00) return new Tree();
		 */
		return null;
	}
	
	public void addTile(Tile t) {
		// this.tiles.add(t);
	}
	
	// ---------------------------------------------------------------------------------------
	// Private methods
	
	private void setCurrentAreaSector(int i) {
		this.currentAreaSectorID = i;
	}
}
