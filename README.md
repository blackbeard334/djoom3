# djoom3 - a Doom 3 Java port.

#### 31-12-19
Version 0.0.9

This release works with the demo assets available in the gamershell.com version of the demo(crc B4FA48EC). To enable you need to set **ID_DEMO_BUILD=true**, command line version would be something like **-D**ID_DEMO_BUILD=true.

As usual, this release broke some stuff...but let's not talk about that.

Other fixes include:
* some major sound fixes  
    * like NPC voices that weren't played correctly
    * or other sounds that were stopping way too early due to some vorbis mania
* some MipMaping issues were "partially" fixed, allowing HQ configs to look less...horrible  
* input...enough said
* mirror mirror on the wall...who's the dumbest developer of them all(fixed the mirror headache in the bathroom)
* other stuff I don't remember
  
Hopefully 2020 will have more progress/releases T_T

[http://www.youtube.com/watch?v=ki3fMz796W0](http://www.youtube.com/watch?v=ki3fMz796W0)  
[![Screenshot](http://img.youtube.com/vi/ki3fMz796W0/0.jpg)](http://www.youtube.com/watch?v=ki3fMz796W0 "djoom3 0.0.8")



#### 17-11-18
Version 0.0.8

Can't be bothered to merge and create a release(since the input isn't working in the **lwjgl3 branch**), but this *"release"* was basically till the [b9adea7df00135eda35e98a5610b69b22dc3cf29](https://github.com/blackbeard334/djoom3/commit/b9adea7df00135eda35e98a5610b69b22dc3cf29) commit.

Where to start...many many physics and lighting fixes. **No more wireframes!!!!1One**

Also had to switch to lwjgl3 cuz of some nvidia driver issues that was causing problems(see issue #1 on the never updated issues).

**//TODO** add more changes


[http://www.youtube.com/watch?v=aEantnePSDs](http://www.youtube.com/watch?v=aEantnePSDs)  
[![Screenshot](http://img.youtube.com/vi/aEantnePSDs/0.jpg)](http://www.youtube.com/watch?v=aEantnePSDs "djoom3 0.0.8")



#### 23-12-17
Version 0.0.7

So I was gonna work on fixing a shitload of bugs during christmas, but 20 years ago today, idSoftware open sourced the original Doom. **Thank you** for that. And I'm sure I speak for most of us when I say we're looking forward to seeing the Rage & Doom(reboot) source code!

Too much has happened since the last release, but it's starting to look proper now.
Still, lots of bugs with lighting, texture mapping...and blending.

As usual, check the 2nd half of the video for the wireframes.

[http://www.youtube.com/watch?v=Ub3dBjdauCY](http://www.youtube.com/watch?v=Ub3dBjdauCY)  
[![Screenshot](http://img.youtube.com/vi/Ub3dBjdauCY/0.jpg)](http://www.youtube.com/watch?v=Ub3dBjdauCY "djoom3 0.0.7")


#### 23-05-16
Version 0.0.6

**ingame BANZAI!**

Too dark, and lots and LOTS of graphics bugs though, but who cares.

Check the 2nd half of the video for the wireframe rendering, to see what's actually going on.

[http://www.youtube.com/watch?v=vudRn5F0Z3w](http://www.youtube.com/watch?v=vudRn5F0Z3w)  
[![Screenshot](http://img.youtube.com/vi/vudRn5F0Z3w/0.jpg)](http://www.youtube.com/watch?v=vudRn5F0Z3w "djoom3 0.0.6")


#### 23-03-16
Version 0.0.5

[http://www.youtube.com/watch?v=JstU-T1mHPo](http://www.youtube.com/watch?v=JstU-T1mHPo)  
[![Screenshot](http://img.youtube.com/vi/JstU-T1mHPo/0.jpg)](http://www.youtube.com/watch?v=JstU-T1mHPo "djoom3 0.0.5")


#### 05-03-16
Version 0.0.4

[http://www.youtube.com/watch?v=TuR7x7iYVJU](http://www.youtube.com/watch?v=TuR7x7iYVJU)  
[![Screenshot](http://img.youtube.com/vi/TuR7x7iYVJU/0.jpg)](http://www.youtube.com/watch?v=TuR7x7iYVJU "djoom3 0.0.4")




#### 05-02-15:
-Initial non playable release.

![https://raw.githubusercontent.com/blackbeard334/djoom3/wiki/init_menus.png](https://raw.githubusercontent.com/blackbeard334/djoom3/wiki/init_menus.png)

![https://raw.githubusercontent.com/blackbeard334/djoom3/wiki/title.png](https://raw.githubusercontent.com/blackbeard334/djoom3/wiki/title.png)




----------

#### Enable OpenAL
Add/edit the following settings to the **DoomConfig.cfg** file:

- seta s_useOpenAL "1"
- seta s_libOpenAL "openal.dylib"

#### How to build:
TODO

#### How to run in an IDE:
##### IntelliJ
1. [https://www.jetbrains.com/idea/help/opening-reopening-and-closing-projects.html](https://www.jetbrains.com/idea/help/opening-reopening-and-closing-projects.html)
2. [Create a run configuration](https://www.jetbrains.com/idea/help/creating-and-editing-run-debug-configurations.html) for **neo.sys.win_main.java** 
3. Add **-Djava.library.path=/bla/natives**(location of the lwjgl native dll/so files) to the VM parameters of the [run configuration] (https://www.jetbrains.com/idea/help/setting-configuration-options.html)
4. Add **"+set fs_basepath '~/.local/share/Steam/steamapps/common/doom 3' +set com_allowConsole 1 +set si_pure 0"**(with quotes) to the program arguments of the [run configuration] (https://www.jetbrains.com/idea/help/setting-configuration-options.html)

###### Run with demo assets(optional)
To run with the demo assets, you need to add **-DID_DEMO_BUILD=true** to the VM params in step 3 above.

### Important
#### Operator Overloading:
|c++|java|
|---------|------------|
|operator=| oSet(value)|
|operator[]| oSet(x, value)|
|operator[]|oGet(x)|
|operator[][]| oGet(x, y)|
|operator[][]| oSet(x, y, value)|
|operator+| oPlus|
|operator-| oMinus|
|operator*| oMultiply|
|operator/| oDivide|
|operator-()| oNegative|
|operator+=| oPluSet|
|operator-=| oMinSet|
|operator*=| oMulSet|
|operator/=| oDivSet|