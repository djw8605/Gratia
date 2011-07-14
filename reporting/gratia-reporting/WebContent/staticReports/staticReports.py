#!/usr/bin/env python

import sys, os, commands, string, urlparse, urllib
from xml.dom import minidom, Node
from ConfigParser import ConfigParser

def walkTree(node):
    if node.nodeType == Node.ELEMENT_NODE:
        yield node
        for child in node.childNodes:
            for n1 in walkTree(child):
                yield n1

def showNode(node, reportsFolder, staticFolder, birtURL):
    if node.nodeName == "reportItem":
        # get the attributes.
        attrs = node.attributes
        for attrName in attrs.keys():
            if attrName == 'link':
                reportsFolder = reportsFolder + '/'
                urlQuery = attrs.get(attrName).nodeValue
                i = urlQuery.index(']') + 1
                s1 = urlQuery[i:]
                i = s1.index('.') + 1
                reportFile = os.path.join(staticFolder, s1[:i] + 'pdf')
                ## print reportFile
                callURL = birtURL + '/' + attrs.get(attrName).nodeValue.replace('[ReportsFolder]', reportsFolder)
                #print ('URL \n%s\n' % (callURL))
                try:
                    f = urllib.urlopen(callURL)
                    w = open(reportFile, 'w')
                    w.write(f.read())
                    w.close
                    f.close
                except IOError, e:
                    sys.exit(1)
    if node.nodeName == "csvItem":
        # get the attributes.
        attrs = node.attributes
        fileName = 'file.csv'
        for attrName in attrs.keys():
            if attrName == 'name':
                name = attrs.get(attrName).nodeValue
                fileName = name.replace(' ', '') + '.csv'
        for attrName in attrs.keys():
            if attrName == 'link':
                reportsFolder = reportsFolder + '/'
                reportFile = os.path.join(staticFolder, fileName)
                ## print reportFile
                callURL = birtURL + '/' + attrs.get(attrName).nodeValue.replace('[csvFileName]', reportFile)
                #print ('URL \n%s\n' % (callURL))
                try:
                    f = urllib.urlopen(callURL)
                    f.close
                except IOError, e:
                    sys.exit(1)

def parseFile(inFileName, reportsFolder, staticFolder, birtURL):
    doc = minidom.parse(inFileName)
    rootNode = doc.documentElement
    for node in walkTree(rootNode):
        showNode(node, reportsFolder, staticFolder, birtURL)

def main():
    args = sys.argv[1:]
    if len(args) != 2:
        print 'usage: ./staticReports.py catalina.home reportsURL(form: http://gratiaxxx:8881/gratia-reporting)'
        sys.exit(1)
    config = ConfigParser()
    catalinaHome = args[0]
    birtURL = args[1]
    propertiesFile = os.path.join(catalinaHome,'gratia/service-configuration.properties')
    config.read(propertiesFile)
    reportsFolder = os.path.join(catalinaHome, 'webapps', config.get( 'gratia', 'service.reporting.reports.folder'))
    exists = os.path.exists(reportsFolder)
    if exists != 1:
        print ('Reports folder %s does not exist' %(reportsFolder))
        sys.exit(1)
    staticConfig = os.path.join(catalinaHome, 'webapps', config.get( 'gratia', 'service.reporting.staticreports'))
    exists = os.path.exists(staticConfig)
    if exists != 1:
        print ('Static Reports configuration file \n %s \n does not exist' %(staticConfig))
        sys.exit(1)
    staticFolder = os.path.join(catalinaHome, 'webapps', config.get( 'gratia', 'service.reporting.static.folder'))
    try:
        os.mkdir(staticFolder)
    except OSError, e:
        pass
    exists = os.path.exists(staticFolder)
    if exists != 1:
        print ('Static Reports folder \n    %s \ndoes not exist and cannot be made' %(staticFolder))
        sys.exit(1)
    #print('StaticConfig %s\nreports folder %s\n static folder %s\n birtURL %s\n' %(staticConfig, reportsFolder, staticFolder, birtURL))
    parseFile(staticConfig, reportsFolder, staticFolder, birtURL)

if __name__ == '__main__':
    main()
