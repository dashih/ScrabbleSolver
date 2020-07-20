# ScrabbleSolver
Helper tool to cheat at Scrabble =)

## Performance and Parallelism
For 7 letters including 2 blanks, modern hardware has no problem producing a complete solution in seconds. But for larger inputs, the problem becomes interesting from a performance/parallelism perspective.

To parallelize, we first generate all the combinations for the input. Then we simply use parallelStream() to permute all combinations.

## Benchmarks
Speedup numbers!

### Intel Core i9-9880H 8-core (2019 Macbook Pro 16-inch)
Speedup = 4x
```
$ time java -jar ScrabbleSolver-5.2.1.jar --input="*ABCDE*FGHI" --min-characters=8 --regex="A.+" --sequential
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
Processed 5,533,760,661
java -jar ScrabbleSolver-5.2.1.jar --input="*ABCDE*FGHI" --min-characters=8    512.33s user 1.08s system 100% cpu 8:30.86 total

$ time java -jar ScrabbleSolver-5.2.1.jar --input="*ABCDE*FGHI" --min-characters=8 --regex="A.+"             
Input: *ABCDE*FGHI (11 length, 2 blanks)
Running in parallel mode
Outputting matches of 8 length or greater
Outputting matches of pattern: A.+
********************************************************************************
ABIDANCE                                                                        
ACIDHEAD                                                                        
ABDICATE                                                                        
********************************************************************************
Found 2,677 solutions for *ABCDE*FGHI
Processed 5,533,760,661
java -jar ScrabbleSolver-5.2.1.jar --input="*ABCDE*FGHI" --min-characters=8   1823.32s user 4.05s system 1431% cpu 2:07.67 total
```
