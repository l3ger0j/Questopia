## QuestPlayAndroid (fork **[QuestPlayer](https://github.com/seedhartha/QuestPlayer)**)

![android-test](https://github.com/l3ger0j/QuestPlayerAndroid/actions/workflows/android-test.yml/badge.svg)

![android-build](https://github.com/l3ger0j/QuestPlayerAndroid/actions/workflows/android-build.yml/badge.svg)

Android application for launching text format games [QSP](https://github.com/seedhartha/qsp).

**This fork was made WhoYouAndM3. The original project (hereinafter **[QuestPlayer](https://github.com/seedhartha/QuestPlayer)**) with an no license does not bear any responsibility for this fork **[QuestPlayAndroid](https://github.com/l3ger0j/QuestPlayer.git)**.**

The differences between this fork and the parent fork are the same, except:
1. Updating dependencies to current versions;
2. Added the ability to "disable" the separator
3. Added a separate ProgressDialog for downloading game and for loading list games
4. Added the "fork" item in "About Quest Player Android"


## Unrecoverable bugs

In this section, bugs will be described, the correction of which requires rewriting the library or other actions that completely paralyze the further development of this project for a significant or indefinite time.

1. **Dynamic work with objects. Example:**
```
$ob=$selobj
if $ob='<img src="pic/flower.png" height="<<item_height>>"><<$text_flower>>':
	unselect
	gs 'GetTempLocation',$curloc
	goto 'Flower'
end
```