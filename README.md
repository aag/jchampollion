JChampollion
============

JChampollion is a Java implementation of the Champollion program described by
Smadja, McKeown and Hatzivassiloglou in this paper:
 
[Smadja, F., McKeown, K. R., and Hatzivassiloglou, V. 1996. Translating
collocations for bilingual lexicons: a statistical approach. Comput. Linguist. 22, 
1 (Mar. 1996), 1-38.](http://dl.acm.org/citation.cfm?id=234287&coll=Portal&dl=ACM)

JChampollion accepts a sentence-aligned, bilingual corpus and a collocation
in the source text (such as those produced by Xtract) and produces a
translation of the collocation in the target language. What's a
collocation? A collocation is defined as "recurrent combinations of words
that co-occur more often than expected by chance and that correspond to
arbitrary word usages." Basically, they're groups of words that often go
together and usually mean something different when together than when
they're apart. Examples are "The United Nations" and "Natural Language
Processing".

The original Champollion was written for English as the source text and
French as the target text, and used the Hansards Corpus for evaluation.
JChampollion uses English as the source language and German as the target
language. Development and testing was done with the
[Europarl Corpus](http://www.statmt.org/europarl/).

Here are some example translations produced by JChampollion:

<table>
    <thead>
        <tr>
            <th>Source Language Collocation</th>
            <th>JChampollion Output</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Madam President</td>
            <td>frau präsidentin</td>
        </tr>
        <tr>
            <td>member states</td>
            <td>mitgliedstaaten</td>
        </tr>
        <tr>
            <td>the committee on agriculture and rural development</td>
            <td>landwirtschaft ländliche</td>
        </tr>
        <tr>
            <td>report on competition policy</td>
            <td>wettbewerbspolitik</td>
        </tr>
    </tbody>
</table>

JChampollion was implemented as part of a grad school semester project. More
information about that project is available at
[the project page](http://definingterms.com/projects/Champollion/).


Limitations
-----------
JChampollion is as close to the original implementation of Champollion as
could be achieved from reading the paper describing its algorithm. The only
detail that is left vague concerns closed class words. The authors of the
paper mention that they do not return closed class words from the target
language in their translations, because their frequency messes up the
statistical correlation data for the rest of the corpus. However, they
don't specify exactly which closed class words they exclude. In JChampollion,
most of the German articles and prepositions are excluded (with some
morphological differences accounted for), but nothing else. Nevertheless,
the lack of prepositions and articles greatly reduces the accuracy of the
translations.

The index files for the corpus are rather large, about 50% larger than the
corpus itself. Ideally the index would be kept in memory, but as the corpus
size grows, this becomes impractical. So, the index is loaded from disk.


Usage
-----
First, make sure you have Java installed. Then, after cloning the repository,
you'll need to build the software. JChampollion uses Gradle Wrapper, so
you can build everything with this command:

    $ ./gradlew installDist

You'll need a sentence-aligned English-German corpus. Development was
done with files from the [Europarl Corpus](http://www.statmt.org/europarl/).

Once you've built the software and you have corpus files, you can run
JChampollion. Running it without any arguments will print some help information:

    $ ./build/install/JChampollion/bin/JChampollion

The first time you run JChampollion on a given corpus, you must include the
`-index` argument, so the corpus will be indexed. You must also tell
JChampollion where to find the source and target files, as well as which
collocation should be translated.

    ./build/install/JChampollion/bin/JChampollion -source ./ep-00-en.txt -target ./ep-00-de.txt -co "member state" -index

On subsequent runs you don't need to include the `-index` argument, as long
as the corpus doesn't change.

    ./build/install/JChampollion/bin/JChampollion -source ./ep-00-en.txt -target ./ep-00-de.txt -co "member state"


License
-------
This code is free software licensed under the GPL v3.  See the COPYING file
for details.
