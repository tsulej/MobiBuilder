
package pl.tsl.mobi


def c = [
	'html' : [ ['h1','a.html','Startowa'], ['h2', 'b.html','Druga strona'], ['h3', 'c.html','Strona 3 i ostatnia']],
	'sections' : [ ['Sekcja A', ['h1','h2']], ['Sekcja B',['h3']] ],
	'imgs' : ['i1' : '1.jpg', 'i2' : '2.gif']
]

MobiBuilder b = new MobiBuilder("testowa")
b.base_path = 'simple/'
b.periodical = true
b.content = c
b.creator = 'Tomasz Sulej'
b.date = '2011-12-09'
b.title = 'Testowa książka'
b.language = 'pl'
b.description = 'łódź'
b.generateBook()
