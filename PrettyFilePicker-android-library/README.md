# PrettyFilePicker
## _Lightweight filepicker in AlertDialog_

[![Build Status](https://travis-ci.org/joemccann/dillinger.svg?branch=master)](https://travis-ci.org/joemccann/dillinger)

PrettyFilePicker launches in a separate alert dialog box 
and you no longer have to catch the return file from the intent!

## Features PrettyFilePicker 0.0.3

- Changed filepicking to Callback(LiveData in previous)
- Support MIME filtering
- Added return as file option
- Added simplify usage

## How to use

> MIME: Array<String> 
```sh
PrettyFilePicker(
    activity: Activty, 
    "Title of filepick", 
    returnAsFile: Boolean, 
    mime: Array<String> ).runFilePicker { data -> //return data as Any
                    val file = data as DocumentFile
                    println(file.name)
                }
```
