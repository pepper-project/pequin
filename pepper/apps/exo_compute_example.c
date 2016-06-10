#include <stdint.h>

#define NUM_ELM 32

struct In { bool flag; uint32_t reg0; uint32_t reg1; uint32_t pc; uint32_t values[NUM_ELM]; };
struct Out { int value; };

struct transcript_elm { 
    uint32_t values[NUM_ELM];
};

void bar(uint32_t in,uint32_t *out);
void stringTest(const char *baz);

void compute(struct In *input, struct Out *output){
    int i;
    struct transcript_elm theTranscript[1];

    uint32_t *exo0_inputs[1] = {input->values};
    uint32_t lens[1] = {NUM_ELM};

    output->value = 1;

    exo_compute(exo0_inputs,lens,theTranscript,0);

    for (i=1; i<NUM_ELM; i++) {
        if (theTranscript[0].values[i] < theTranscript[0].values[i-1]) {
            output->value = 0;
        }
    }
}
