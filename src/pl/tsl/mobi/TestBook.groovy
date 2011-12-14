
package pl.tsl.mobi


def c = [
	'html' : [ ['h1','a.html','Startowa'], ['h2', 'b.html','Druga strona'], ['h3', 'c.html','Strona 3 i ostatnia']], // order is important 
	'sections' : [ ['Sekcja A', ['h1','h2']], ['Sekcja B',['h3']] ], // order is important
	'imgs' : ['i1' : '1.jpg', 'i2' : '2.gif'],
	'mbpmeta' : ['h1' : ['author 1', 'description 1'], 'h2' : ['author 2','description 2'], 'h3' : ['author 3','description 3']]
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
