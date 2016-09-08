#!/usr/bin/env python
# -*- coding: utf-8 -*-
import matplotlib.pyplot as plt, sys

def plotArray(y):
	plt.figure()
	plt.boxplot(y, 0, '')
	plt.ylabel('Delay in ms')
	plt.show()	

def parseFiletoArray(filename):
	file = open(filename)
	dictionary = {}
	for line in file:
		string = line[3:].replace("\n", "")
		array = string.split('___')
		tmp = list(array[0])
		tmp[-1] = '0'
		tmp[-2] = '0'
		tmp[-3] = '0'
		tmp[-4] = '0'
		dictionary["".join(tmp)] = array[1] 
	return dictionary

def reduceOffset(inDic, offset):
	outDic = {}
	for entry in inDic:
		outDic[entry] = str(int(inDic[entry])-int(offset[entry]));
	return outDic

def main(args):
	timerOffset = parseFiletoArray(args[1]);
	data = []
	for files in args[2:]:
		rawDic = parseFiletoArray(files)
		dic = reduceOffset(rawDic, timerOffset)
		keys = []
		values = []
		for entry in dic:
			keys.append(int(entry))
			values.append(int(dic[entry]))
		data.append(values)
	plotArray(data)



if __name__ == "__main__":
    main(sys.argv[:])
