package pl.tsl.mobi.esensja

import pl.tsl.mobi.MobiBuilder

def esensja = new EditionBuilder('esres/','es109h/2011/07/')
//def esensja = new EditionBuilder('esres/','2011','08')
def content = esensja.parseEdition()

println content

MobiBuilder b = new MobiBuilder('esensja201108')
b.cover_name = 'img/cover.jpg'
b.masthead_name = 'img/masthead.jpg'
b.base_path = 'esres/'
b.periodical = true
b.content = content
b.publisher = 'Esensja'
b.creator = 'Esensja'
b.date = '2011-08'
b.title = 'Esensja 08.2011'
b.language = 'pl'
b.subject = 'Esensja,Newsy'
b.description = 'Magazyn Esensja 08.2011'
b.generateBook()
