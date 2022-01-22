// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)
//
// This program only needs to handle arguments that satisfy
// R0 >= 0, R1 >= 0, and R0*R1 < 32768.

// Put your code here.
  // product = 0
  @product
  M=0
  // i = R0
  @R0
  D=M
  @i
  M=D
(LOOP)
  // i = i - 1
  @i
  M=M-1
  // if (i < 0) goto STOP
  @i
  D=M
  @STOP
  D;JLT
  // product = product + R1
  @product
  D=M
  @R1
  D=D+M
  @product
  M=D
  // goto LOOP
  @LOOP
  0;JMP
(STOP)
  // R2 = product
  @product
  D=M
  @R2
  M=D
(END)
  @END
  0;JMP