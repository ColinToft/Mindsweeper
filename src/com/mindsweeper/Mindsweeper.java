package com.mindsweeper;

import java.util.Random;
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;

public class Mindsweeper {
	private static final int MAINMENU = 0;
	private static final int PLAYING = 1;
	
	private static int boardWidth = 8;
	private static int boardHeight = 8;
	private static int mines = 10;
	
	private static int playerX = 0;
	private static int playerY = 0;
	
	private final static int SUPERTINY = 0;
	private final static int EASY = 1;
	private final static int MEDIUM = 2;
	private final static int HARD = 3;
	private final static int CUSTOM = 4;

	private static int screen = MAINMENU;
	private static int difficulty = SUPERTINY;
	
	private static boolean worldLoaded;
	private static byte[][] board;
	private static boolean[][] showing;
	private static boolean[][] flagged;
	
	private static int squaresRevealed;
	private static boolean gameFinished;
	
	private static Font font;

	public static void main(String[] args) {
		
		while (true) {
			
			if (screen == MAINMENU) {
				drawMainMenu();
			} else if (screen == PLAYING) {
				drawGameScreen();
			}
		}
		
		
	}
	
	public static void centerX(GraphicsLCD g, String str, int y) {
		g.drawString(str, 89, y, GraphicsLCD.HCENTER | GraphicsLCD.BASELINE);
	}
	
	public static void drawMainMenu() {
		
		GraphicsLCD g = LocalEV3.get().getGraphicsLCD();
		
		g.clear();
		
		g.setFont(Font.getDefaultFont());
		
		centerX(g, "Mindsweeper", 15);
		
		if (difficulty == SUPERTINY) {
			centerX(g, "[Super Tiny]", 45);
		} else {
			centerX(g, "Super Tiny", 45);
		}
		if (difficulty == EASY) {
			centerX(g, "[Easy]", 65);
		} else {
			centerX(g, "Easy", 65);
		}
		if (difficulty == MEDIUM) {
			centerX(g, "[Medium]", 85);
		} else {
			centerX(g, "Medium", 85);
		}
		if (difficulty == HARD) {
			centerX(g, "[Hard]", 105);
		} else {
			centerX(g, "Hard", 105);
		}
		if (difficulty == CUSTOM) {
			centerX(g, "[Custom]", 125);
		} else {
			centerX(g, "Custom", 125);
		}
		
		g.setFont(Font.getSmallFont());
		
		centerX(g, "Select a Difficulty", 27);
		
		switch (Button.waitForAnyPress()) {
			case Button.ID_UP:
				if (difficulty > 0) difficulty--;
				break;
			case Button.ID_DOWN:
				if (difficulty < 4) difficulty++;
				break;
			case Button.ID_ENTER:
				screen = PLAYING;
				switch (difficulty) {
					case SUPERTINY:
						boardWidth = 4;
						boardHeight = 4;
						mines = 3;
						break;
					case EASY:
						boardWidth = 8;
						boardHeight = 8;
						mines = 10;
						break;
					case MEDIUM:
						boardWidth = 12;
						boardHeight = 12;
						mines = 23;
						break;
					case HARD:
						boardWidth = 16;
						boardHeight = 16;
						mines = 53;
						break;
					case CUSTOM:
						boardWidth = 4;
						boardHeight = 4;
						mines = 3;
						drawCustomSelection();
						if (screen == MAINMENU) {
							return;
						}	
				}
				worldLoaded = false;
				showing = new boolean[boardWidth][boardHeight];
				flagged = new boolean[boardWidth][boardHeight];
				playerX = 0; playerY = 0;
				squaresRevealed = 0;
				adjustFont();
				break;
			default:
				System.exit(0);
		}
	}
	
	public static void drawCustomSelection() {
		
		GraphicsLCD g = LocalEV3.get().getGraphicsLCD();
		
		g.setFont(Font.getLargeFont());
		
		int selected = 0;
		
		while (true) {
		
			g.clear();
			
			g.setFont(selected == 0 ? Font.getLargeFont() : Font.getDefaultFont());
			g.drawString("Width: " + Integer.toString(boardWidth), 0, 32, GraphicsLCD.BASELINE | GraphicsLCD.LEFT);
			
			g.setFont(selected == 1 ? Font.getLargeFont() : Font.getDefaultFont());
			String start = "Height: ";
			if (boardHeight > 9 && selected == 1) {
				start = "Height:";
			}
			g.drawString(start + Integer.toString(boardHeight), 0, 64, GraphicsLCD.BASELINE | GraphicsLCD.LEFT);
			
			g.setFont(selected == 2 ? Font.getLargeFont() : Font.getDefaultFont());
			start = "Mines: ";
			if (mines > 99 && selected == 2) {
				start = "Mines:";
			}
			g.drawString(start + Integer.toString(mines), 0, 96, GraphicsLCD.BASELINE | GraphicsLCD.LEFT);
			
			g.setFont(Font.getSmallFont());
			g.drawString("Press enter to play.", 89, 116, GraphicsLCD.BASELINE | GraphicsLCD.HCENTER);
			
			switch (Button.waitForAnyPress()) {
				case Button.ID_UP:
					selected--;
					break;
				case Button.ID_DOWN:
					selected++;
					break;	
				case Button.ID_RIGHT:
					switch (selected) {
						case 0:
							boardWidth++;
							break;
						case 1:	
							boardHeight++;
							break;
						case 2:
							mines++;
							break;
					}
					break;
				case Button.ID_LEFT:
					switch (selected) {
						case 0:
							boardWidth--;
							break;
						case 1:	
							boardHeight--;
							break;
						case 2:
							mines--;
							break;
					}
					break;
				case Button.ID_ESCAPE:
					screen = MAINMENU;
				default:
					return;
			}
			
			boardWidth = clamp(boardWidth, 1, 44);
			boardHeight = clamp(boardHeight, 1, 18);
			mines = clamp(mines, 0, boardWidth * boardHeight);
			selected = clamp(selected, 0, 2);
		}	
	}
	
	public static void drawGameScreen() {

		GraphicsLCD g = LocalEV3.get().getGraphicsLCD();
		
		g.clear();
		
		int x, y;
		
		float squareWidth = 178F / boardWidth;
		float squareHeight = 128F / boardHeight;
		
		
		for (int i = 1; i < boardWidth; i++) {
			x = (int)(squareWidth * i);
			g.drawLine(x, 0, x, 128);
		}
		for (int i = 1; i < boardHeight; i++) {
			y = (int)(squareHeight * i);
			g.drawLine(0, y, 178, y);
		}
		
		g.drawRect((int)((squareWidth * playerX) - 1), (int)((squareHeight * playerY) - 1), (int)(squareWidth * (playerX + 1)) - ((int)(squareWidth * playerX) - 2), (int)(squareHeight * (playerY + 1)) - ((int)(squareHeight * playerY) - 2));
		
		g.setFont(font);
		
		for (int i = 0; i < boardWidth; i++) {
			for (int j = 0; j < boardHeight; j++) {
				x = (int)((squareWidth * i) + (squareWidth / 2F));
				y = (int)((squareHeight * j) + (squareHeight / 2F));
				if (showing[i][j] && board[i][j] != 9) {
					g.drawString(Byte.toString(board[i][j]), x, (y - font.height / 2) + 1, GraphicsLCD.HCENTER);
				} else if (flagged[i][j]) {
					g.drawString("!", x, (y - font.height / 2) + 1, GraphicsLCD.HCENTER);
				} else if (showing[i][j] && board[i][j] == 9) {
					g.drawString("*", x, (y - font.height / 2) + 1, GraphicsLCD.HCENTER);
				}
			}
		}
		
		int pressed = Button.waitForAnyPress();
		
		if (gameFinished) return;
		
		switch (pressed) {
			case Button.ID_LEFT:
				playerX--;
				break;
			case Button.ID_RIGHT:
				playerX++;
				break;
			case Button.ID_UP:
				playerY--;
				break;
			case Button.ID_DOWN:
				playerY++;
				break;
			case Button.ID_ESCAPE:
				Button.LEDPattern(3);
				flagged[playerX][playerY] = !flagged[playerX][playerY];
				Button.LEDPattern(0);
				break;
			case Button.ID_ENTER:
				if (!worldLoaded) {
					board = generateBoard(boardWidth, boardHeight, mines);
					worldLoaded = true;
				}
				if (flagged[playerX][playerY]) {
					return;
				}
				if (board[playerX][playerY] == 9) {
					g.clear();
					g.setFont(Font.getLargeFont());
					g.drawString("You lost!", 89, 48, GraphicsLCD.HCENTER);
					Button.LEDPattern(5);
					Button.waitForAnyPress(3000);
					revealBoard();
				} else {
					if (board[playerX][playerY] == 0) {
						expand(playerX, playerY);
					}
					if (!showing[playerX][playerY]) {
						squaresRevealed++;
						showing[playerX][playerY] = true;
					} else {
						clearAround(playerX, playerY);
					}
					if (squaresRevealed + mines == boardWidth * boardHeight) {
						g.clear();
						g.setFont(Font.getLargeFont());
						g.drawString("You won!", 89, 48, GraphicsLCD.HCENTER);
						Button.LEDPattern(4);
						Button.waitForAnyPress(3000);
						revealBoard();
					}
				}
				break;
			default:
				screen = MAINMENU;
		}
		playerX = clamp(playerX, 0, boardWidth - 1);
		playerY = clamp(playerY, 0, boardHeight - 1);
	}
	
	public static int clamp(int value, int min, int max) {
		if (value < min) value = min;
		if (value > max) value = max;
		return value;
	}
	
	public static byte[][] generateBoard(int width, int height, int mines) {
		
		byte[][] board = new byte[width][height];
		
		boolean protectSquare = width * height > mines + 8;
		
		boolean protectPlayer = width * height > mines;
		
		Random r = new Random();
		int currentX, currentY;
		byte mc;
		
		for (int i = 0; i < mines; i++) {
			do {
				currentX = r.nextInt(width);
				currentY = r.nextInt(height);
			} while (board[currentX][currentY] == 9 || (protectSquare && (Math.abs(playerX - currentX) < 2 && Math.abs(playerY - currentY) < 2)) || (protectPlayer && playerX == currentX && playerY == currentY));
			board[currentX][currentY] = 9;
		}
		
		
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				mc = 0;
				for (int x = i - 1; x < i + 2; x++) {
					for (int y = j - 1; y < j + 2; y++) {
						if ((i != x || j != y) && (-1 < x) && (x < width) && (-1 < y) && (y < height)) {
							if (board[x][y] == 9) mc++;
						}
					}
				}
				if (board[i][j] != 9) {
					board[i][j] = mc;
				}
			}
		}
		
		return board;
		
	}
	
	public static void clearAround(int squareX, int squareY) {
		
		int flaggedCount = 0;
		for (int x = squareX - 1; x < squareX + 2; x++) {
			for (int y = squareY - 1; y < squareY + 2; y++) {
				if ((squareX != x || squareY != y) && (-1 < x) && (x < boardWidth) && (-1 < y) && (y < boardHeight)) {
					if (flagged[x][y]) flaggedCount++;
				}
			}
		}
		if (flaggedCount >= board[squareX][squareY]) {
			for (int x = squareX - 1; x < squareX + 2; x++) {
				for (int y = squareY - 1; y < squareY + 2; y++) {
					if ((squareX != x || squareY != y) && (-1 < x) && (x < boardWidth) && (-1 < y) && (y < boardHeight) && (!showing[x][y])) {
						if (!flagged[x][y]) {
							showing[x][y] = true;
							if (board[x][y] == 0) {
								expand(x, y);
							}
							squaresRevealed++;
							if (board[x][y] == 9) {
								GraphicsLCD g = LocalEV3.get().getGraphicsLCD();
								g.clear();
								g.setFont(Font.getLargeFont());
								g.drawString("You lost!", 89, 48, GraphicsLCD.HCENTER);
								Button.waitForAnyPress(3000);
								revealBoard();
							}
						}	
					}
				}
			}
		}	
		
	}
	
	public static void adjustFont() {
		
		if (boardWidth > 25 || boardHeight > 14) {
			font = new TinyFont();
		} else if (boardWidth > 16 || boardHeight > 8) {
			font = Font.getSmallFont();
		} else if (boardWidth > 8 || boardHeight > 3) {
			font = Font.getDefaultFont();
		} else {
			font = Font.getLargeFont();
		}
	
	}
	
	public static void expand(int x, int y) {
	    if (board[x][y] == 0) {
	    	if (!showing[x][y]) {
	    		showing[x][y] = true;
	    		squaresRevealed++;
	    	}
	    	for (int expandX = x - 1; expandX < x + 2; expandX++) {
	    		for (int expandY = y - 1; expandY < y + 2; expandY++) {
	    			if (expandY > -1 && expandY < boardHeight && expandX > -1 && expandX < boardWidth && !flagged[expandX][expandY]) {
	    				if (board[expandX][expandY] != 9 && board[expandX][expandY] > 0 && !showing[expandX][expandY]) {
	    					showing[expandX][expandY] = true;
	    					squaresRevealed++;
	    				} else if ((expandX != x || expandY != y) && board[expandX][expandY] == 0 && !showing[expandX][expandY]) {
	    					expand(expandX, expandY);
	    				}
	    			}
	    		} 	
	    	}
	    }
	}
	
	public static void revealBoard() {
		for (int i = 0; i < boardWidth; i++) {
			for (int j = 0; j < boardHeight; j++) {
				showing[i][j] = true;
				flagged[i][j] = false;
			}
		}
		gameFinished = true;
		drawGameScreen();
		worldLoaded = false;
		gameFinished = false;
		Button.LEDPattern(0);
		screen = MAINMENU;
	}
}


class TinyFont extends Font {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7778038990105368925L;

	TinyFont() {
        super(new byte[] {(byte) 0x40, (byte) 0xaa, (byte) 0x24, (byte) 0x64, (byte) 0x24, 
                        (byte) 0x0a, (byte) 0x00, (byte) 0x80, (byte) 0x4c, (byte) 0x66, 
                        (byte) 0xe8, (byte) 0xe4, (byte) 0x44, (byte) 0x00, (byte) 0x08, 
                        (byte) 0x62, (byte) 0x4e, (byte) 0xc6, (byte) 0xe6, (byte) 0xce, 
                        (byte) 0xea, (byte) 0xa8, (byte) 0xa2, (byte) 0x4a, (byte) 0x46, 
                        (byte) 0xc6, (byte) 0xae, (byte) 0xaa, (byte) 0xaa, (byte) 0x6e, 
                        (byte) 0x62, (byte) 0x04, (byte) 0x06, (byte) 0x02, (byte) 0x08, 
                        (byte) 0x08, (byte) 0x42, (byte) 0x24, (byte) 0x04, (byte) 0x00, 
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                        (byte) 0xc0, (byte) 0x64, (byte) 0x0a, (byte) 0x40, (byte) 0xea, 
                        (byte) 0x8c, (byte) 0x2a, (byte) 0x42, (byte) 0x44, (byte) 0x00, 
                        (byte) 0x80, (byte) 0x6a, (byte) 0x88, (byte) 0x2a, (byte) 0x82, 
                        (byte) 0xaa, (byte) 0x00, (byte) 0x04, (byte) 0x84, (byte) 0xaa, 
                        (byte) 0x2a, (byte) 0x2a, (byte) 0x22, (byte) 0x4a, (byte) 0xa8, 
                        (byte) 0xe2, (byte) 0xae, (byte) 0xaa, (byte) 0x2a, (byte) 0xa4, 
                        (byte) 0xaa, (byte) 0xaa, (byte) 0x28, (byte) 0x42, (byte) 0x0a, 
                        (byte) 0x04, (byte) 0x02, (byte) 0x08, (byte) 0x04, (byte) 0x02, 
                        (byte) 0x20, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x44, 
                        (byte) 0x05, (byte) 0x40, (byte) 0xa0, (byte) 0x46, (byte) 0x0c, 
                        (byte) 0x42, (byte) 0xee, (byte) 0xe0, (byte) 0x40, (byte) 0x4a, 
                        (byte) 0x44, (byte) 0x6e, (byte) 0xc6, (byte) 0xc4, (byte) 0x44, 
                        (byte) 0xe2, (byte) 0x48, (byte) 0xea, (byte) 0x26, (byte) 0x6a, 
                        (byte) 0xa6, (byte) 0x4e, (byte) 0x68, (byte) 0xe2, (byte) 0xaa, 
                        (byte) 0xa6, (byte) 0xe6, (byte) 0xa4, (byte) 0xea, (byte) 0x44, 
                        (byte) 0x24, (byte) 0x44, (byte) 0x00, (byte) 0xc0, (byte) 0xc6, 
                        (byte) 0xec, (byte) 0xee, (byte) 0x46, (byte) 0xa4, (byte) 0xe4, 
                        (byte) 0x46, (byte) 0xc6, (byte) 0xc6, (byte) 0xae, (byte) 0xaa, 
                        (byte) 0xaa, (byte) 0x66, (byte) 0xc4, (byte) 0x00, (byte) 0x00, 
                        (byte) 0xe0, (byte) 0x2c, (byte) 0x0a, (byte) 0x42, (byte) 0x44, 
                        (byte) 0x00, (byte) 0x20, (byte) 0x4a, (byte) 0x82, (byte) 0x88, 
                        (byte) 0x4a, (byte) 0x8a, (byte) 0x00, (byte) 0x04, (byte) 0x04, 
                        (byte) 0xa2, (byte) 0x2a, (byte) 0x2a, (byte) 0xa2, (byte) 0x4a, 
                        (byte) 0xaa, (byte) 0xa2, (byte) 0xaa, (byte) 0xe2, (byte) 0x8a, 
                        (byte) 0xa4, (byte) 0xea, (byte) 0x4a, (byte) 0x22, (byte) 0x48, 
                        (byte) 0x00, (byte) 0xa0, (byte) 0x2a, (byte) 0x6a, (byte) 0xa4, 
                        (byte) 0x4a, (byte) 0x64, (byte) 0xe4, (byte) 0xaa, (byte) 0xaa, 
                        (byte) 0x42, (byte) 0xa4, (byte) 0xea, (byte) 0xa4, (byte) 0x44, 
                        (byte) 0x44, (byte) 0x00, (byte) 0x40, (byte) 0xa0, (byte) 0x86, 
                        (byte) 0x0e, (byte) 0x24, (byte) 0x0a, (byte) 0x06, (byte) 0x24, 
                        (byte) 0xe6, (byte) 0x6e, (byte) 0x68, (byte) 0x44, (byte) 0x44, 
                        (byte) 0x64, (byte) 0xe8, (byte) 0x42, (byte) 0xae, (byte) 0xc6, 
                        (byte) 0xe6, (byte) 0xc2, (byte) 0xea, (byte) 0xa4, (byte) 0xae, 
                        (byte) 0x4a, (byte) 0xc2, (byte) 0x6a, (byte) 0xe4, (byte) 0xa4, 
                        (byte) 0x4a, (byte) 0x6e, (byte) 0x68, (byte) 0x00, (byte) 0xe0, 
                        (byte) 0xc6, (byte) 0xcc, (byte) 0x84, (byte) 0x4a, (byte) 0xa4, 
                        (byte) 0xa4, (byte) 0x4a, (byte) 0xc6, (byte) 0x62, (byte) 0xec, 
                        (byte) 0xe4, (byte) 0x4a, (byte) 0xcc, (byte) 0x64, (byte) 0x00, 
                        (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, 
                        (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x00, 
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                        (byte) 0x00, (byte) 0xf0, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                        (byte) 0xe0, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, 
                        (byte) 0x82, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20, 
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, },
                4, 6, 5, 4, 96, 32);
	}
        
}

