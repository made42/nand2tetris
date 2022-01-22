// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.
(PROGLOOP)
  @8191
  D=A
  @n
  M=D  // n = 256 * 512 - 1

  @i
  M=0  // i = 0

  @KBD
  D=M
  @EMPTY
  D;JEQ
  @FILL
  0;JMP

(EMPTY)
  @color
  M=0
  @DRAW
  0;JMP

(FILL)
  @color
  M=-1
  @DRAW
  0;JMP

(DRAW)
  @SCREEN
  D=A
  @address
  M=D  // address = 16384 (base address of the Hack screen)

(DRAWLOOP)
  @i
  D=M
  @n
  D=D-M
  @END
  D;JGT   // if i>n goto END

  @color
  D=M
  @address
  A=M
  M=D    // RAM[address] = color (16 pixels)

  @i
  M=M+1  // i = i + 1
  @address
  M=M+1  // address = address + 1
  @DRAWLOOP
  0;JMP  // goto DRAWLOOP

(END)
  @PROGLOOP
  0;JMP  // goto PROGLOOP