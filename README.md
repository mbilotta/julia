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

### On Windows

Windows users should just download and run `julia.exe` (latest release [here](https://github.com/mbilotta/julia/releases/latest)). SmartScreen may complain about the signature so youl'll have to click _More info_ and then _Run anyway_. If no JRE 1.8+ installation is found, a popup will give instructions to download and install the latest version of Java.

### On other OSes

You need JRE/JDK 1.8+ installed in your system. After installing Java, you should be able to run Julia simply double clicking on `julia.jar`. Alternatively, you can run Julia from the command line by typing:

```
java -jar julia.jar
```

provided you have placed `julia.jar` in your home directory. Also note that the command line is not only accessible through terminal emulators/command prompt. Most OSes/desktops provide [Run dialogs](https://en.wikipedia.org/wiki/Run_command) that hide once you issued a command.

### Missing plugins error

First time Julia is started you will get an error about missing plugins. That's because Julia doesn't come equipped with any of the pluggable elements discussed above. You can install [DPC4J](https://github.com/mbilotta/dpc4j) that provides a minimal set of plugins to start exploring fractals.

## Building Julia

You need JDK 1.8+ and Maven installed in your system:

    git clone https://github.com/mbilotta/julia.git
    cd julia
    mvn clean package

## Acknowledgements

Thanks to [Robert Munafo](https://mrob.com/pub/index.html) for providing valuable informations on fractal rendering algorithms througout its [Mu-Ency](https://mrob.com/pub/muency.html) site.

## Supporting this project

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=FMKKXRVLLGYWU&item_name=Support+Julia%3A+The+Fractal+Generator&currency_code=EUR&source=url)

## Licensing information

Julia is provided under the terms of the GNU Lesser General Public License (LGPL), ver. 3. You can read the license terms clicking the _About Julia_ button in the dialog you see the first time it runs or you can open _Help → About Julia_ in the main UI.

This program is distributed in the hope that will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILIY or FITNESS FOR A PARTICULAR PURPOSE. For more details, refer to the specific license.
