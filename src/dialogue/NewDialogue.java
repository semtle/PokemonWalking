package dialogue;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import main.Keys;
import main.MainComponent;
import resources.Art;
import screen.BaseScreen;
import abstracts.Tile;
import entity.Player;

public class NewDialogue {
	public static final int DIALOGUE_QUESTION = 0x41;
	public static final int DIALOGUE_SPEECH = 0x40;
	public static final int HALF_STRING_LENGTH = 9;
	// Dialogue max string length per line.
	public static final int MAX_STRING_LENGTH = 18;
	private ArrayList<String> completedLines;
	private Keys input;
	private int lineIterator;
	private int lineLength;
	private ArrayList<Map.Entry<String, Boolean>> lines;
	private boolean nextFlag;
	private boolean simpleQuestionFlag;
	private boolean yesNoCursorPosition;
	private byte nextTick;
	private int scrollDistance;
	private boolean scrollFlag;
	private boolean showDialog;
	private boolean simpleQuestionAnswerFlag;
	
	private int subStringIterator;
	private byte tickCount = 0x0;
	
	private int totalDialogueLength;
	private int type;
	
	private NewDialogue(Keys keys) {
		lines = new ArrayList<Map.Entry<String, Boolean>>();
		completedLines = new ArrayList<String>(3);
		this.subStringIterator = 0;
		this.lineLength = 0;
		this.totalDialogueLength = 0;
		this.nextFlag = false;
		this.simpleQuestionFlag = false;
		this.scrollFlag = false;
		this.scrollDistance = 0;
		this.nextTick = 0x0;
		this.lineIterator = 0;
		this.input = keys;
		this.showDialog = false;
		this.type = 0;
		this.yesNoCursorPosition = true;
	}
	
	public boolean dialogBoxIsShowing() {
		return this.showDialog;
	}
	
	public void render(BaseScreen output, Graphics graphics) {
		render(output, graphics, 0, 6, 9, 2);
	}
	
	public void render(BaseScreen output, Graphics graphics, int x, int y, int w, int h) {
		if (x < 0)
			x = 0;
		if (x > 9)
			x = 9;
		if (y < 0)
			y = 0;
		if (y > 8)
			y = 8;
		if (x + w > 9)
			w = 9 - x;
		if (y + h > 8)
			h = 8 - y;
		if (showDialog) {
			switch (this.type) {
				case DIALOGUE_SPEECH: {
					renderDialogBackground(output, x, y, w, h);
					renderDialogBorderBox(output, x, y, w, h);
					if (this.nextFlag && this.nextTick < 0x8)
						output.blit(Art.dialogue_next, MainComponent.GAME_WIDTH - 16, MainComponent.GAME_HEIGHT - 8);
					Graphics2D g2d = output.getBufferedImage().createGraphics();
					renderText(g2d);
					g2d.dispose();
					graphics.drawImage(MainComponent.createCompatibleBufferedImage(output.getBufferedImage()), 0, 0, MainComponent.COMPONENT_WIDTH, MainComponent.COMPONENT_HEIGHT, null);
					break;
				}
				case DIALOGUE_QUESTION: {
					renderDialogBackground(output, x, y, w, h);
					renderDialogBorderBox(output, x, y, w, h);
					if (this.simpleQuestionFlag && !this.nextFlag) {
						renderDialogBackground(output, 7, 3, 2, 2);
						renderDialogBorderBox(output, 7, 3, 2, 2);
						//Offset by -3 for the Y axis. 
						output.blit(Art.dialogue_pointer, MainComponent.GAME_WIDTH - Tile.WIDTH * 3 + 8, yesNoCursorPosition ? (Tile.HEIGHT * 4 - 3) : (Tile.HEIGHT * 5 - 3));
					}
					else if (!this.simpleQuestionFlag && (this.nextFlag && this.nextTick < 0x8))
						output.blit(Art.dialogue_next, MainComponent.GAME_WIDTH - 16, MainComponent.GAME_HEIGHT - 8);
					Graphics2D g2d = output.getBufferedImage().createGraphics();
					renderText(g2d);
					renderYesNoAnswerText(g2d);
					g2d.dispose();
					graphics.drawImage(MainComponent.createCompatibleBufferedImage(output.getBufferedImage()), 0, 0, MainComponent.COMPONENT_WIDTH, MainComponent.COMPONENT_HEIGHT, null);
					break;
				}
			}
		}
	}
	
	public boolean textIsCreated() {
		return !this.lines.isEmpty();
	}
	
	public void tick() {
		if (this.subStringIterator < this.totalDialogueLength && (!this.nextFlag && !this.scrollFlag)) {
			tickCount++;
			if (tickCount > 0x1)
				tickCount = 0x0;
		}
		else if (this.nextFlag) {
			switch (this.type) {
				case DIALOGUE_QUESTION:
					this.simpleQuestionFlag = true;
					this.nextFlag = false;
					this.scrollFlag = false;
					break;
				case DIALOGUE_SPEECH:
					this.nextTick++;
					if (this.nextTick > 0xE)
						this.nextTick = 0x0;
					break;
			}
		}
		else if (this.subStringIterator >= this.totalDialogueLength) {
			if (this.lineIterator > this.lines.size()) {
				switch (this.type) {
					case DIALOGUE_QUESTION:
						this.simpleQuestionFlag = true;
						this.scrollFlag = false;
						this.nextFlag = false;
						break;
					case DIALOGUE_SPEECH:
						this.closeDialog();
						return;
				}
			}
			Map.Entry<String, Boolean> entry = this.lines.get(this.lineIterator);
			this.completedLines.add(entry.getKey());
			this.lineIterator++;
			switch (this.type) {
				case DIALOGUE_SPEECH:
					this.nextFlag = true;
					break;
				case DIALOGUE_QUESTION:
					this.simpleQuestionFlag = true;
					this.nextFlag = false;
					break;
			}
		}
		
		try {
			
			if (!this.nextFlag && !this.simpleQuestionFlag && !this.scrollFlag) {
				if (tickCount == 0x0) {
					if (!this.scrollFlag)
						this.subStringIterator++;
					if (this.subStringIterator >= this.lineLength) {
						this.subStringIterator %= this.lineLength;
						Map.Entry<String, Boolean> entry = this.lines.get(this.lineIterator);
						this.completedLines.add(entry.getKey());
						this.lineIterator++;
					}
					if (this.completedLines.size() == 2) {
						if (!this.scrollFlag) {
							switch (this.type) {
								case DIALOGUE_SPEECH:
									this.nextFlag = true;
									break;
								case DIALOGUE_QUESTION:
									//Must get to the end of the entire dialogue before asking for answers.
									if (this.subStringIterator >= this.totalDialogueLength)
										this.simpleQuestionFlag = true;
									else
										this.nextFlag = true;
									break;
							}
						}
					}
				}
				if (input.Z.keyStateDown && !(input.Z.lastKeyState)) {
					input.Z.lastKeyState = true;
					tickCount = 0x0;
				}
			}
			else if (this.simpleQuestionFlag && !this.nextFlag && !this.scrollFlag) {
				//Making sure this doesn't trigger the "Next" arrow.
				this.nextFlag = false;
				
				if ((this.input.up.keyStateDown && !this.input.up.lastKeyState) || (this.input.W.keyStateDown && !this.input.W.lastKeyState)) {
					this.input.up.lastKeyState = true;
					this.input.W.lastKeyState = true;
					//Made it consistent with Inventory's menu selection, where it doesn't wrap around.
					this.yesNoCursorPosition = true;
				}
				else if ((this.input.down.keyStateDown && !this.input.down.lastKeyState) || (this.input.S.keyStateDown && !this.input.S.lastKeyState)) {
					this.input.down.lastKeyState = true;
					this.input.S.lastKeyState = true;
					//Made it consistent with Inventory's menu selection, where it doesn't wrap around.
					this.yesNoCursorPosition = false;
				}
				if ((this.input.Z.keyStateDown && !this.input.Z.lastKeyState) || (this.input.SLASH.keyStateDown && !this.input.SLASH.lastKeyState)) {
					this.input.Z.lastKeyState = true;
					this.input.SLASH.lastKeyState = true;
					this.yesNoCursorPosition = true;
					this.simpleQuestionFlag = false;
					this.simpleQuestionAnswerFlag = true; //Confirmed
					this.closeDialog();
				}
				else if ((this.input.X.keyStateDown && !this.input.X.lastKeyState) || (this.input.PERIOD.keyStateDown && !this.input.PERIOD.lastKeyState)) {
					this.input.X.lastKeyState = true;
					this.input.PERIOD.lastKeyState = true;
					this.yesNoCursorPosition = true;
					this.simpleQuestionFlag = false;
					this.simpleQuestionAnswerFlag = false; //Rejected.
					this.closeDialog();
				}
			}
			else {
				if (input.Z.keyStateDown && !(input.Z.lastKeyState)) {
					input.Z.lastKeyState = true;
					switch (this.type) {
						case DIALOGUE_SPEECH:
							this.nextFlag = false;
							this.scrollFlag = true;
							break;
						case DIALOGUE_QUESTION:
							//Must get to the end of the entire dialogue before asking questions.
							this.simpleQuestionFlag = false;
							this.nextFlag = false;
							this.scrollFlag = true;
							break;
					}
				}
				if (this.scrollFlag) {
					if (this.lineIterator >= this.lines.size()) {
						switch (this.type) {
							case DIALOGUE_QUESTION:
								this.simpleQuestionFlag = true;
								this.scrollFlag = false;
								this.nextFlag = false;
								break;
							case DIALOGUE_SPEECH:
								this.closeDialog();
								return;
						}
					}
					else
						this.scrollDistance += 8;
				}
			}
		}
		catch (Exception e) {
			if (this.lineIterator >= this.lines.size()) {
				if (this.scrollFlag) {
					this.closeDialog();
				}
				else {
					switch (this.type) {
						case DIALOGUE_SPEECH:
							this.nextFlag = true;
							break;
						case DIALOGUE_QUESTION:
							this.simpleQuestionFlag = true;
							break;
					}
				}
			}
		}
	}
	
	public int getDialogueType() {
		return this.type;
	}
	
	private void closeDialog() {
		this.showDialog = false;
		if (!this.lines.isEmpty())
			this.lines.clear();
	}
	
	private void renderText(Graphics g) {
		final int X = 8;
		final int Y1 = 120;
		final int Y2 = 136;
		final Rectangle rect = new Rectangle(X, Y1 - Tile.HEIGHT * 2, MainComponent.GAME_WIDTH, MainComponent.GAME_HEIGHT);
		
		g.setFont(Art.font.deriveFont(8f));
		g.setColor(Color.black);
		
		String string = null;
		try {
			switch (this.completedLines.size()) {
				case 0:
					// None completed.
					string = this.lines.get(this.lineIterator).getKey();
					if (this.subStringIterator > string.length()) {
						g.drawString(string.substring(0, string.length()), X, Y1);
						this.subStringIterator = this.lineLength;
					}
					else
						g.drawString(string.substring(0, this.subStringIterator), X, Y1);
					break;
				case 1:
					// One line completed.
					g.drawString(this.completedLines.get(0), X, Y1);
					string = this.lines.get(this.lineIterator).getKey();
					if (this.subStringIterator > string.length()) {
						g.drawString(string.substring(0, string.length()), X, Y2);
						this.subStringIterator = this.lineLength;
					}
					else
						g.drawString(string.substring(0, this.subStringIterator), X, Y2);
					break;
				case 2:
					// Two lines completed.
					if (!this.scrollFlag) {
						g.drawString(this.completedLines.get(0), X, Y1);
						g.drawString(this.completedLines.get(1), X, Y2);
					}
					else {
						// Time to scroll.
						// DEBUG: Needs testing to see if there's any problem with
						// it.
						Graphics g_clipped = g.create();
						g_clipped.setClip(rect.x, rect.y, rect.width, rect.height);
						g_clipped.drawString(this.completedLines.get(0), X, Y1 - scrollDistance);
						g_clipped.drawString(this.completedLines.get(1), X, Y2 - scrollDistance);
						if (tickCount == 0x0) {
							if (scrollDistance >= Y2 - Y1) {
								this.scrollFlag = false;
								this.scrollDistance = 0;
								this.subStringIterator = 0;
								this.completedLines.remove(0);
								this.lines.get(this.lineIterator).setValue(true);
							}
						}
						g_clipped.dispose();
					}
					break;
			}
		}
		catch (Exception e) {
		}
		
	}
	
	public void renderYesNoAnswerText(Graphics g) {
		g.setFont(Art.font.deriveFont(8f));
		g.setColor(Color.black);
		
		final int X = Tile.WIDTH * 8;
		final int YES_HEIGHT = Tile.HEIGHT * 4 + 4;
		final int NO_HEIGHT = Tile.HEIGHT * 5 + 4;
		try {
			g.drawString("YES", X, YES_HEIGHT);
			g.drawString("NO", X, NO_HEIGHT);
		}
		catch (Exception e) {
		}
	}
	
	public static NewDialogue createText(String dialogue, int length, int type) {
		NewDialogue dialogues = new NewDialogue(MainComponent.getMainInput());
		dialogues.lines = toLines(dialogue, length);
		dialogues.lineLength = length;
		dialogues.totalDialogueLength = dialogue.length();
		dialogues.type = type;
		dialogues.showDialog = true;
		if (!Player.isMovementsLocked())
			Player.lockMovements();
		return dialogues;
	}
	
	public static void renderDialogBox(BaseScreen output, int x, int y, int centerWidth, int centerHeight) {
		output.blit(Art.dialogue_top_left, x * Tile.WIDTH, y * Tile.HEIGHT);
		for (int i = 0; i < centerWidth - 1; i++) {
			output.blit(Art.dialogue_top, ((x + 1) * Tile.WIDTH) + (i * Tile.WIDTH), y * Tile.HEIGHT);
		}
		output.blit(Art.dialogue_top_right, (x + centerWidth) * Tile.WIDTH, y * Tile.HEIGHT);
		
		for (int j = 0; j < centerHeight - 1; j++) {
			output.blit(Art.dialogue_left, x * Tile.WIDTH, ((y + 1) * Tile.HEIGHT) + j * Tile.HEIGHT);
			for (int i = 0; i < centerWidth - 1; i++) {
				output.blit(Art.dialogue_background, ((x + 1) * Tile.WIDTH) + (i * Tile.WIDTH), ((y + 1) * Tile.HEIGHT) + j * Tile.HEIGHT);
			}
			output.blit(Art.dialogue_right, (x + centerWidth) * Tile.WIDTH, ((y + 1) * Tile.HEIGHT) + j * Tile.HEIGHT);
		}
		
		output.blit(Art.dialogue_bottom_left, x * Tile.WIDTH, ((y + centerHeight) * Tile.HEIGHT));
		for (int i = 0; i < centerWidth - 1; i++) {
			output.blit(Art.dialogue_bottom, ((x + 1) * Tile.WIDTH) + (i * Tile.WIDTH), ((y + centerHeight) * Tile.HEIGHT));
		}
		output.blit(Art.dialogue_bottom_right, (x + centerWidth) * Tile.WIDTH, ((y + centerHeight) * Tile.HEIGHT));
	}
	
	private static void renderDialogBackground(BaseScreen output, int x, int y, int centerWidth, int centerHeight) {
		for (int j = 0; j < centerHeight - 1; j++) {
			for (int i = 0; i < centerWidth - 1; i++) {
				output.blit(Art.dialogue_background, ((x + 1) * Tile.WIDTH) + (i * Tile.WIDTH), ((y + 1) * Tile.HEIGHT) + j * Tile.HEIGHT);
			}
		}
	}
	
	private static void renderDialogBorderBox(BaseScreen output, int x, int y, int centerWidth, int centerHeight) {
		output.blit(Art.dialogue_top_left, x * Tile.WIDTH, y * Tile.HEIGHT);
		for (int i = 0; i < centerWidth - 1; i++) {
			output.blit(Art.dialogue_top, ((x + 1) * Tile.WIDTH) + (i * Tile.WIDTH), y * Tile.HEIGHT);
		}
		output.blit(Art.dialogue_top_right, (x + centerWidth) * Tile.WIDTH, y * Tile.HEIGHT);
		
		for (int j = 0; j < centerHeight - 1; j++) {
			output.blit(Art.dialogue_left, x * Tile.WIDTH, ((y + 1) * Tile.HEIGHT) + j * Tile.HEIGHT);
			output.blit(Art.dialogue_right, (x + centerWidth) * Tile.WIDTH, ((y + 1) * Tile.HEIGHT) + j * Tile.HEIGHT);
		}
		
		output.blit(Art.dialogue_bottom_left, x * Tile.WIDTH, ((y + centerHeight) * Tile.HEIGHT));
		for (int i = 0; i < centerWidth - 1; i++) {
			output.blit(Art.dialogue_bottom, ((x + 1) * Tile.WIDTH) + (i * Tile.WIDTH), ((y + centerHeight) * Tile.HEIGHT));
		}
		output.blit(Art.dialogue_bottom_right, (x + centerWidth) * Tile.WIDTH, ((y + centerHeight) * Tile.HEIGHT));
	}
	
	private static ArrayList<Map.Entry<String, Boolean>> toLines(String all, final int regex) {
		ArrayList<Map.Entry<String, Boolean>> lines = new ArrayList<>();
		String[] words = all.split("\\s");
		String line = "";
		int length = 0;
		for (String w : words) {
			if (length + w.length() + 1 > regex) {
				if (w.length() >= regex) {
					line += w;
					lines.add(new AbstractMap.SimpleEntry<String, Boolean>(line, false));
					line = "";
					continue;
				}
				lines.add(new AbstractMap.SimpleEntry<String, Boolean>(line, false));
				line = "";
				length = 0;
			}
			if (length > 0) {
				line += " ";
				length += 1;
			}
			line += w;
			length += w.length();
		}
		if (line.length() > 0)
			lines.add(new AbstractMap.SimpleEntry<String, Boolean>(line, false));
		return lines;
	}
	
}
