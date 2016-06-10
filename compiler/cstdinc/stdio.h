//Currently, not supported.

#ifndef _STDIO_H
# define _STDIO_H	1

#ifndef STDOUT_SIZE
# define STDOUT_SIZE	100
#endif

#ifndef STDIN_SIZE
# define STDIN_SIZE	100
#endif

#ifndef EOF
# define EOF		-1
#endif

//Note - these variable names are specially interpreted by ZCC.
int __stdin_buffer[STDOUT_SIZE]; //int so that EOF can be a value
int __stdout_buffer[STDOUT_SIZE]; //int so that EOF can be a value
int __stdin_pos = 0;
int __stdout_pos = 0;

int getchar(void){
  return __stdin_buffer[__stdin_pos++];
}
int putchar(int character){
  //"The value is internally converted to an unsigned char when written"
  return __stdout_buffer[__stdout_pos++] = (unsigned char)character;
}
/*
int printf(const char* format, ...){
  //....
}
*/

#endif /* !_STDIO_H */
