package pl.tsl.mobi.esensja

import pl.tsl.mobi.MobiBuilder

def esensja = new EditionBuilder('esres/','es109h/2011/07/')
//def esensja = new EditionBuilder('esres/','2011','08')
def content = esensja.parseEdition()