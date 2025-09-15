### TODO List
* add verifierTools construction :heavy_check_mark: 
* check other approaches to response verification
* finish & run agent :heavy_check_mark: 
* finish other tool sets & modes
* add some kind of backtracking?
* addCodeSnippet fails sometimes

## !! Make error message fixed

### What could be interesting to read?
* https://arxiv.org/pdf/2411.07112
* https://arxiv.org/abs/2501.16207
* just another new paper https://arxiv.org/pdf/2502.05714
* 

### Run Configuration
```
--grazie-token
# insert token
--llm-profile
gpt4.1
--tries
5
--runs
1
--filter-by-ext
py
--output-logging
--dir
benches/HumanEval-Nagini/Bench
--modes
mode1
--prompts
prompts/nagini/mode1/mode1-1
--max-jobs
5
--verifier-command
"rm -rf .mypy_cache; nagini"
--results-path
results
--agentic-tools
...
```
