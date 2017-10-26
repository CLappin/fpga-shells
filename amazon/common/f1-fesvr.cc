// See LICENSE.SiFive for license details.
// See LICENSE.Berkeley for license details.

#include <fesvr/f1_dtm.h>
#include <iostream>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <getopt.h>

f1_dtm_t* f1_dtm;
static uint64_t trace_count = 0;
bool verbose;
bool done_reset;

void handle_sigterm(int sig)
{
  f1_dtm->stop();
}

double sc_time_stamp()
{
  return trace_count;
}

static void usage(const char * program_name) {
  printf("Usage: %s [OPTION]... BINARY [BINARY ARGS]\n", program_name);
  fputs("\
Run a BINARY on the Rocket Chip emulator.\n\
\n\
Mandatory arguments to long options are mandatory for short options too.\n\
  -c, --cycle-count          print the cycle count before exiting\n\
       +cycle-count\n\
  -h, --help                 display this help and exit\n\
  -m, --max-cycles=CYCLES    kill the emulation after CYCLES\n\
       +max-cycles=CYCLES\n\
  -s, --seed=SEED            use random number seed SEED\n\
  -V, --verbose              enable all Chisel printfs\n\
       +verbose\n\
", stdout);
#if VM_TRACE
  fputs("\
  -v, --vcd=FILE,            write vcd trace to FILE (or '-' for stdout)\n\
  -x, --dump-start=CYCLE     start VCD tracing at CYCLE\n\
      +dump-start\n\
", stdout);
#else
  fputs("\
VCD options (e.g., -v, +dump-start) require a debug-enabled emulator.\n\
Try `make debug`.\n\
", stdout);
#endif
}

int main(int argc, char** argv)
{
  unsigned random_seed = (unsigned)time(NULL) ^ (unsigned)getpid();
  uint64_t max_cycles = -1;
  int ret = 0;
  bool print_cycles = false;
#if VM_TRACE
  FILE * vcdfile = NULL;
  uint64_t start = 0;
#endif

  std::vector<std::string> to_dtm;
  while (1) {
    static struct option long_options[] = {
      {"cycle-count", no_argument,       0, 'c' },
      {"help",        no_argument,       0, 'h' },
      {"max-cycles",  required_argument, 0, 'm' },
      {"seed",        required_argument, 0, 's' },
      {"verbose",     no_argument,       0, 'V' },
#if VM_TRACE
      {"vcd",         required_argument, 0, 'v' },
      {"dump-start",  required_argument, 0, 'x' },
#endif
      {0, 0, 0, 0}
    };
    int option_index = 0;
#if VM_TRACE
    int c = getopt_long(argc, argv, "-chm:s:v:Vx:", long_options, &option_index);
#else
    int c = getopt_long(argc, argv, "-chm:s:V", long_options, &option_index);
#endif
    if (c == -1) break;
    switch (c) {
      // Process "normal" options with '--' long options or '-' short options
      case '?': usage(argv[0]);             return 1;
      case 'c': print_cycles = true;        break;
      case 'h': usage(argv[0]);             return 0;
      case 'm': max_cycles = atoll(optarg); break;
      case 's': random_seed = atoi(optarg); break;
      case 'V': verbose = true;             break;
#if VM_TRACE
      case 'v': {
        vcdfile = strcmp(optarg, "-") == 0 ? stdout : fopen(optarg, "w");
        if (!vcdfile) {
          std::cerr << "Unable to open " << optarg << " for VCD write\n";
          return 1;
        }
        break;
      }
      case 'x': start = atoll(optarg);      break;
#endif
      // Processing of legacy '+' options and recognition of when
      // we've hit the binary. The binary is expected to be a
      // non-option and not start with '-' or '+'.
      case 1: {
        std::string arg = optarg;
        if (arg == "+verbose")
          verbose = true;
        else if (arg.substr(0, 12) == "+max-cycles=")
          max_cycles = atoll(optarg+12);
#if VM_TRACE
        else if (arg.substr(0, 12) == "+dump-start=")
          start = atoll(optarg+12);
#endif
        else if (arg.substr(0, 12) == "+cycle-count")
          print_cycles = true;
        else {
          to_dtm.push_back(optarg);
          goto done_processing;
        }
        break;
      }
    }
  }

done_processing:
  if (optind < argc)
    while (optind < argc)
      to_dtm.push_back(argv[optind++]);
  if (!to_dtm.size()) {
    std::cerr << "No binary specified for emulator\n";
    usage(argv[0]);
    return 1;
  }

  if (verbose)
    fprintf(stderr, "using random seed %u\n", random_seed);

  srand(random_seed);
  srand48(random_seed);

  // the dut better be setup
  f1_dtm = new f1_dtm_t(to_dtm);

  signal(SIGTERM, handle_sigterm);

  // reset happens at power-on in f1
  //f1_dtm.write(reset)
  done_reset = true;

  // Can't check success and trace count is inaccurate
  while (!f1_dtm->done() && trace_count < max_cycles) {
    trace_count++;
  }

  if (f1_dtm->exit_code())
  {
    fprintf(stderr, "*** FAILED *** (code = %d, seed %d) after %ld cycles\n", f1_dtm->exit_code(), random_seed, trace_count);
    ret = f1_dtm->exit_code();
  }
  else if (trace_count == max_cycles)
  {
    fprintf(stderr, "*** FAILED *** (timeout, seed %d) after %ld cycles\n", random_seed, trace_count);
    ret = 2;
  }
  else if (verbose || print_cycles)
  {
    fprintf(stderr, "Completed after %ld cycles\n", trace_count);
  }

  if (f1_dtm) delete f1_dtm;
  return ret;
}
