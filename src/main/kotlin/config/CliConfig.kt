package org.example.config

import java.nio.file.Path


data class CliConfig(
    val token: String,                   // --grazie-token
    val llmProfile: Model,                   // --llm-profile (string name)
//    val benchTypes: List<String>,              // --bench-types=a,b
    val tries: Int,                            // --tries 10
    val runs: Int,                             // --runs 5
    val filterByExt: Extensions,                  // --filter-by-ext py
    val outputLogging: Boolean,                // --output-logging (flag)
    val dir: Path,                           // --dir benches/.../Bench
    val modes: List<Modes>,                   // --modes=mode5,mode6
    val promptsDirectories: List<Path>,      // --prompts-directory=a,b
    val temperature: Double,                   // --temperature=0.3
    val maxJobs: Int,                          // --max-jobs=5
    val verifierCommand: String,               // --verifier-command="..."
    val toolsPerMode: Map<Modes, Set<AgenticTools>>,
    val resultsPath: Path,
    val checkers: List<CheckerArt>,
    val maxIterations: Int,
    val isApplication: Boolean,
    val trainingDataPath: Path? = null,
    val corpusPath: Path? = null,
)