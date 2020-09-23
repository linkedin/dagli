# Java Version Requirement
Dagli currently requires **Java 9** or later (or, equivalently, the corresponding Java Runtime Environment for use with JVM 
languages other than Java).

#### Java 8
Prior to Dagli 14, Dagli supported Java 8, but with Java 8 reaching nominal end-of-life in December 2020, we believe it 
is no longer worthwhile to maintain this.  In particular, moving to Java 9 allows us to use 
`MethodHandles.privateLookupIn(...)` rather than relying on reflection, and over time we will start to take advantage 
of other language improvements (e.g. private interface methods).