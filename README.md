Thera is a static website generator for Scala, similar to [Jekyll] for Ruby or Hakyll for Haskell.

[![asciicast](https://asciinema.org/a/O94xzncXnETSsZ84lPOvC0iQJ.png)](https://asciinema.org/a/O94xzncXnETSsZ84lPOvC0iQJ)

# Installation
1. Make sure that [Docker](https://www.docker.com/get-started/) is installed. If it is not, follow the official documentation to do so.
2. Download Thera executable: `sudo curl -L https://raw.githubusercontent.com/anatoliykmetyuk/thera/master/thera.sh > /usr/local/bin/thera && chmod +x /usr/local/bin/thera`.
3. Run `thera` to ensure a help message gets output.

# Example
```bash
git clone https://github.com/anatoliykmetyuk/thera.git
cd thera/example
thera start  # start Thera Docker container
thera build  # run the build script
open http://localhost:8888  # open the site in browser
thera stop  # stop the Docker container
```

# Usage
Thera consists of two components: the command-line application and the Scala library. The CLI provides a convenient way to build sites with minimal setup. The library provides template processing capabilities.

## CLI
```bash
mkdir thera-test; cd thera-test  # create the base directory for your blog
echo "It works!" > index.html  # create a file we are going to process
echo -e \
"import \$ivy.\`com.github.pathikrit::better-files:3.6.0\`
import better.files._, File._, java.io.{ File => JFile }
file\"index.html\" copyTo file\"_site/index.html\"
" > build.sc  # create an Ammonite-based Thera build script
thera start  # start Thera Docker container
thera build  # run the build script
open http://localhost:8888  # open the site in browser
```

## Library
The main functionality of the library is in the following method, under `thera.template` object:

```scala
def apply(tmlPath: File, initialVars: Json = Json.obj(), templateResolver: (String) ⇒ File = default.templateResolver, fragmentResolver: (String) ⇒ File = default.fragmentResolver, templateFilters: (String) ⇒ TemplateFilter = Map()): Ef[String]
```

`tmlPath` is the target file to run through templating engine (see file format below).
`initialVars` specifies the variables present in scope that you want to use to populate your template.
`templateResolver` specifies how to resolve a template name to a file.
`fragmentResolver` specifies how to resolve a fragment name to a file.
`templateFilters` specifies how to resolve filters you are using in a template.

## Template Format
Here is an example file to be run through the template engine:

```
---
template: html-template
filters: [currentTimeFilter]
variables:
  title: This stuff works!
  one: "1"
  two: "2"
fragments:
  three: three-frag
---
I have numbers #{one} and #{two}. If I add them, here is what I get: #{three}.
```

The contents between `---` lines at the top is the YAML header. It has the following sections defined (all optional, the header is also optional itself):

- `template` - the template to wrap the body (below the header) into. You can refer to the body from a template using `#{body}` variable.
- `filters` - the filters you are going to run your body through. Filters are `String => Either[String, String]` - left is an error, right is the result of the transformation.
- `variables` - variables are used in the body and the templates via the `#{var_name}` syntax.
- `fragments` - fragments are fetched using fragment resolvers by value (right-hand-side of the `:` in each fragment declaration) and referred to from the body and the template as variables, by name `#{fragment_name}`.
