# ScrabbleSolver
Helper tool to cheat at Scrabble =)

## Performance
For 7 letters including 2 blanks, modern hardware has no problem producing a complete solution in seconds. But for larger inputs, the problem becomes interesting from a performance/parallelism perspective.

### Parallelization strategy
First generate all the combinations for the input. Use parallelStream() to permute all combinations.

Additionally, if a given combination is large enough, attempt to parallelize the actual permuting. Consider each character in the string and swap it to the front to create a starting point. Then parallelize permuting these starting points, leaving the first character alone.

## Benchmarks
Speedup numbers!

### Intel(R) Xeon(R) Platinum 8275CL CPU @ 3.00GHz 48-core (~2020 EC2 c5.24xlarge)
Speedup = 16.7x
```
$ time java -jar ScrabbleSolver-6.1.1.jar --input="*ABCDE*FGHI" --min-characters=8 --regex="A.+" --sequential
Input: *ABCDE*FGHI (11 length, 2 blanks)
Running in sequential mode
Outputting matches of 8 length or greater
Outputting matches of pattern: A.+
********************************************************************************
ACIDHEAD                                                                        
ABIDANCE                                                                        
ABDICATE                                                                        
********************************************************************************
Found 2,677 solutions for *ABCDE*FGHI
Processed 5,533,760,661 permutations

real	9m12.635s
user	9m20.070s
sys	0m1.682s

$ time java -jar ScrabbleSolver-6.1.1.jar --input="*ABCDE*FGHI" --min-characters=8 --regex="A.+"
Input: *ABCDE*FGHI (11 length, 2 blanks)
Running in parallel mode
Outputting matches of 8 length or greater
Outputting matches of pattern: A.+
********************************************************************************
ACIDHEAD                                                                        
ABIDANCE                                                                        
ABDICATE                                                                        
********************************************************************************
Found 2,677 solutions for *ABCDE*FGHI
Processed 5,533,760,661 permutations

real	0m33.175s
user	42m36.708s
sys	0m20.816s
```

### Intel Core i9-9880H 8-core (2019 Macbook Pro 16-inch)
Speedup = 4.62x
```
$ time java -jar ScrabbleSolver-6.1.1.jar --input="*ABCDE*FGHI" --min-characters=8 --regex="A.+" --sequential
Input: *ABCDE*FGHI (11 length, 2 blanks)
Running in sequential mode
Outputting matches of 8 length or greater
Outputting matches of pattern: A.+
********************************************************************************
ACIDHEAD                                                                        
ABIDANCE                                                                        
ABDICATE                                                                        
********************************************************************************
Found 2,677 solutions for *ABCDE*FGHI
Processed 5,533,760,661 permutations
java -jar ScrabbleSolver-6.1.1.jar --input="*ABCDE*FGHI" --min-characters=8    682.83s user 3.85s system 100% cpu 11:25.90 total

$ time java -jar ScrabbleSolver-6.1.1.jar --input="*ABCDE*FGHI" --min-characters=8 --regex="A.+"             
Input: *ABCDE*FGHI (11 length, 2 blanks)
Running in parallel mode
Outputting matches of 8 length or greater
Outputting matches of pattern: A.+
********************************************************************************
ABDICATE                                                                        
ACIDHEAD                                                                        
ABIDANCE                                                                        
********************************************************************************
Found 2,677 solutions for *ABCDE*FGHI
Processed 5,533,760,661 permutations
java -jar ScrabbleSolver-6.1.1.jar --input="*ABCDE*FGHI" --min-characters=8   2253.61s user 5.27s system 1516% cpu 2:28.96 total
```
