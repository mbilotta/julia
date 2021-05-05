[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0) [![picocli](https://img.shields.io/badge/picocli-4.0.0-green.svg)](https://github.com/remkop/picocli) [![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=FMKKXRVLLGYWU&item_name=Support+Julia%3A+The+Fractal+Generator&currency_code=EUR&source=url)

# Julia: The Fractal Generator

Julia is a fractal generator program completely written in Java. It started as a college work around 2010. Despite its age, this project is still fully functional. Here is a screenshot of Julia running on Windows 10:

![Julia 1.1 running on Windows 10](https://imgur.com/MxmNfxX.png)

Please note that this project focuses only on two-dimensional, escape-time fractals like the Mandelbrot set and the Julia set. A clear and concise definition of such a fractal is that given by [Tom Van Cutsem](http://soft.vub.ac.be/~tvcutsem/teaching/wpo/grafsys/ex5/les5.html):

> Such fractals are computed by repeatedly applying a transformation to a given point in the plane. The obtained series of transformed points is called the orbit of the initial point. An orbit diverges when its points grow further apart without bounds. A fractal can then be defined as the set of points whose orbit does not diverge.

## Main features

### Pluggable architecture

Any image created with Julia results from the combination of 3 pluggable elements:

* __Number factory__. Unlike other programs, Julia does not endorse/embed any specific library for doing arbitrary precision computations. Instead, it is possible to have one or more number factories, each representing a link between Julia and a particular package. This allows users not only to switch from fixed to arbitrary precision arithmetic as they start deep zooming; users will also have the choice to switch from (say) [Apfloat](http://www.apfloat.org/apfloat_java/) to [JScience](http://jscience.org/), or any other library, even native ones, provided the specific number factory is available.

* __Formula__. Any recurrence relation that can generate fractals in the complex plane. Here are some examples: 
    * ![equation](http://latex.codecogs.com/svg.latex?z_{n%2B1}%20%3D%20z_n^2%20%2B%20c)
    * ![equation](http://latex.codecogs.com/svg.latex?z_{n%2B1}%20%3D%20c%20e^{z_n})
    * ![equation](http://latex.codecogs.com/svg.latex?z_{n%2B1}%20%3D%20z_n%20-%20\dfrac{z_n^4%20%2B%20(c%20-%201)%20z_n^2%20-%20c}{4z_n^3%20%2B%202%20(c-1)%20z_n})
    
    Formulas use resources provided by the number factory to carry out computations. Each is capable to generate one Mandelbrot set and infinitely many Julia sets.

* __Representation__. Any possible rendering method/algorithm. To better explain what means using a certain representation instead of another, look at these examples:

  [![Tangent circles](http://mbilotta.altervista.org/wp-content/uploads/2015/02/rep4-150x150.png "Tangent circles")](http://mbilotta.altervista.org/wp-content/uploads/2015/02/rep4.png)
  [![Escape time](http://mbilotta.altervista.org/wp-content/uploads/2015/02/rep2-150x150.png "Escape time")](http://mbilotta.altervista.org/wp-content/uploads/2015/02/rep2.png)
  [![Mu-Ency](http://mbilotta.altervista.org/wp-content/uploads/2015/02/rep3-150x150.png "Mu-Ency")](http://mbilotta.altervista.org/wp-content/uploads/2015/02/rep3.png)
  [![Ring segments](http://mbilotta.altervista.org/wp-content/uploads/2015/02/rep1-150x150.png "Ring segments")](http://mbilotta.altervista.org/wp-content/uploads/2015/02/rep1.png)
  
  These images were generated by Julia applying 4 different representations to the same formula. While other two partners act more like “service providers”, representation is the active element that actually carries out the entire rendering process.

### Multithreaded rendering

The rendering process can be splitted into several concurrent flows in order to take advantage of modern multiprocessor architectures.

### JIM I/O format

Beside common formats like JPEG, PNG etc. (only available as outputs), Julia provides its own file format called JIM (Julia IMage). JIM files can be further edited in Julia; incomplete renderings are saved with the ability to be resumed after opening.

## Running Julia

Being written in Java, Julia requires JRE/JDK 1.8+ to be installed in your system.

### On Windows

Windows users should just download and run `julia.exe` (latest release [here](https://github.com/mbilotta/julia/releases/latest)). SmartScreen may complain about the signature so youl'll have to click _More info_ and then _Run anyway_. If no JRE 1.8+ installation is found, a popup will give instructions to download and install the latest version of Java.

Please note that if you want to use the CLI as well as the GUI you should download `juliac.exe` instead.

### On other OSes

After installing Java, you should be able to run Julia simply double clicking on `julia.jar`. Alternatively, you can run Julia from the command line by typing:

```
java -jar julia.jar
```

Also note that the command line is not only accessible through terminal emulators/command prompt. Most OSes/desktops provide [Run dialogs](https://en.wikipedia.org/wiki/Run_command) that hide once you issued a command.

### Missing plugins error

First time Julia is started you will get an error about missing plugins. That's because Julia doesn't come equipped with any of the pluggable elements discussed above. You can install [DPC4J](https://github.com/mbilotta/dpc4j) that provides a minimal set of plugins to start exploring fractals.

## Command Line Interface (CLI)

Below you'll find some examples of using the Julia CLI to generate images. Examples will make use of `juliac.exe` just for brevity: it's intended that they apply also when using `java -jar julia.jar` to run the program.

### Simplest case

Mandelbrot set of quadratic formula (full view) using the _escape time_ algorithm (all parameters set to default), 800x600 pixels, PNG format:

    juliac.exe generate -n Double -f Quadratic -r EscapeTime -W 800 -H 600 -o mandelbrot.png

At a minimum, we need to specify the number factory (`-n`), the formula (`-f`) and the representation (`-r`) to be used.

To save in a different format, just use a different extension in the output file name.

### Julia set generation

Full view of Julia set at (0.285, 0.01), JPEG format, everything else as above:

    juliac.exe generate -n Double -f Quadratic -r EscapeTime -W 800 -H 600 -o julia.jpg c=0.285,0.01

A "default" Julia set point which depends on the formula being used can be set passing `c=default`.

### Zooming

Back to Mandelbrot set. This is a detail of what is known as _Seahorse Valley_:

    juliac.exe generate -n Double -f Quadratic -r EscapeTime -W 800 -H 600 -o mandelbrot-seahorse.png rect=-0.751085,0.13247425,-0.734975,0.12039175 r.maxIterations=2000

A partial view is set with <code>rect=_Re<sub>0</sub>_,_Im<sub>0</sub>_,_Re<sub>1</sub>_,_Im<sub>1</sub>_</code>. It is important that you specify the argument without whitespaces. Also between left and right hand sides there must be a single `=` without withespaces.

Currently there is no way to set `rect` using center and diameter as you would do in other programs. I will try to address this limitation in the next releases. Anyway it is important to note that by default Julia will force 1:1 pixel ratio enlarging `rect` as much as needed to fit the aspect ratio of the output image. This behaviour can be prevented passing `--no-force-equal-scales`.

Also note that we are raising the maximum number of iterations to avoid loss of accuracy. This is accomplished by `r.maxIterations=2000` which sets the `maxIterations` parameter of the selected representation to a higher value (the default for `EscapeTime` is 500).

### Setting color and gradient parameters

Following example sets the `untrappedOutsidePoint` parameter of `TangentCircles` to a transparent color (the format is <code>_R_,_G_,_B_,_A_</code>), the `untrappedInsidePoint` parameter to an opaque color (alpha is omitted) and the `trappedPoint4` parameter to a gradient starting from color (0, 255, 108) going to (255, 64, 44) and stopping at (0, 0, 96):

    juliac.exe generate -n Double -f Quadratic -r TangentCircles -W 800 -H 600 -o mandelbrot-tc.png r.untrappedOutsidePoint=25,150,82,50 r.untrappedInsidePoint=80,140,200 r.trappedPoint4=0,255,108@0^255,64,44@.6^0,0,96@1

### Using parameter hints

Here we are telling Julia to set the `gradient` parameter of `EscapeTime` to its third hint (note that hints are enumerated starting from zero):

    juliac.exe generate -n Double -f Quadratic -r EscapeTime -W 800 -H 600 -o mandelbrot.png r.hint.gradient=2

### Using hint groups

Hint groups can be recalled similarly as hints but using `*` instead of a parameter name:

    juliac.exe generate -n Double -f Carlson -r TangentCircles -W 800 -H 600 -o mandelbrot-tc-fig5.png r.hint.*=fig5

Hint groups can also be used to set individual parameters. E.g. passing `r.hint.rc=fig5` will set the single parameter `rc` to its value inside group `fig5` (other parameters in the group will be unaffected).

### Setting number factory and formula parameters

You can use the prefixes `n` and `f` respectively:

    juliac.exe generate -n BigDecimal -f Multibrot -r EscapeTime -W 800 -H 600 -o multibrot.png n.precision=32 f.bailout=100

Here `precision` is a parameter of the `BigDecimal` number factory while `bailout` pertains to the `Multibrot` formula.

### Setting equivalent parameters across different plugins

It may happen that formula and representation (e.g.) have equivalent parameters with the exact same name and meaning (e.g.: `bailout`). In this case we might want to set these parameters consistently:

    juliac.exe generate -n Double -f Mandelbrot -r MuEncy -W 800 -H 600 -o muency.png *.bailout=100

### JIM output

To save in JIM format, just append the `.jim` extension to the output file name.

Currently it is not possible to save a partial rendering when using the CLI (it is only possible when using the GUI). I will try to address this limitation in the next releases.

### JIM input

JIM images can be returned to the CLI using the `-i` option. This way you can just convert them to PNG or JPEG:

    juliac.exe generate -i muency.jim -o muency.png

Or you can tweak some parameters and then save to a traditional format (or stick with JIM):

    juliac.exe generate -i muency.jim -o muency-alt.png r.angleWeight=4

You can tweak everything. You can even change a plugin or switch between Mandelbrot set/Julia set but remember that every modification that is different from a change in the value of a _previewable_ parameter will require Julia to repeat the calculation from scratch.

## Building Julia

You need JDK 1.8+ and Maven installed in your system:

    git clone https://github.com/mbilotta/julia.git
    cd julia
    mvn clean package

### Building executable wrappers for Windows

Run the following:

    mvn -P rel clean package

## Credits

Thanks to [Robert Munafo](https://mrob.com/pub/index.html) for providing valuable informations on fractal rendering algorithms througout its [Mu-Ency](https://mrob.com/pub/muency.html) site.

### Icons

Most of the icons were taken/derived from the [Momenticons](https://www.iconfinder.com/iconsets/momenticons-basic) matte set by [Momentum Design Lab](http://momentumdesignlab.com/), licensed under [Creative Commons Attribution 3.0 unported (CC BY 3.0)](http://creativecommons.org/licenses/by/3.0/).

Some other icons were taken from the [Farm-fresh](https://www.iconfinder.com/iconsets/fatcow) set by [FatCow Web Hosting](https://www.fatcow.com/), licensed under [Creative Commons Attribution 3.0 United States (CC BY 3.0 US)](https://creativecommons.org/licenses/by/3.0/us/).

## Supporting this project

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=FMKKXRVLLGYWU&item_name=Support+Julia%3A+The+Fractal+Generator&currency_code=EUR&source=url)

## Licensing information

Julia is provided under the terms of the GNU Lesser General Public License (LGPL), ver. 3. You can read the license terms clicking the _About Julia_ button in the dialog you see the first time it runs or you can open _Help → About Julia_ in the main UI.

This program is distributed in the hope that will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILIY or FITNESS FOR A PARTICULAR PURPOSE. For more details, refer to the specific license.
