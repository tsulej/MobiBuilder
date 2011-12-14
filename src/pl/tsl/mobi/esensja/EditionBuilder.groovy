package pl.tsl.mobi.esensja

import groovy.xml.MarkupBuilder

class EditionBuilder {

	private static final String url_base = 'http://esensja.pl/magazyn/' 
	
	boolean parse_file = false
	boolean simulate = false
	
	String edition_year
	String edition_month
	private String url_path 
	private output_path
	
	def content
	private def parser
	
	private def init(op) {
		this.output_path = op
		parser = new org.cyberneko.html.parsers.SAXParser()
		parser.setFeature('http://xml.org/sax/features/namespaces', false)
	}
	
	EditionBuilder(String output_path, String year, String month) {
		this.edition_year = year
		this.edition_month = month
		url_path = url_base + year + '/' + month + '/'
		
		init(output_path)
	}
	
	EditionBuilder(String output_path, String path) {
		parse_file = true
		url_path = path
		
		init(output_path)
	}
	
	// Parse content from URL (file or web)
	private def parseURL(name) {
		if(parse_file){
		  return new XmlParser(parser).parse(new FileReader(url_path + name))
		} else {
		  return new XmlParser(parser).parse(url_path + name)
		}
	}
	
	// Get content from URL (file or web)
	private InputStream getFile(name) {
		if(parse_file) {
		  return new FileInputStream(url_path + name)
		} else {
		  return new URL(url_path + name).openStream()
		}
	}
	
	// Save content from input stream to file
	private def saveFile(InputStream what, name) {
		println "Saving file: $name"
		if(simulate) return
		def f = new File(output_path + name)
		if(f.exists()) {
			f.delete()
		} 
		f << what
	}
	
	// some consts
	private final static String _cover = 'img/cover.jpg'
	private final static String _masthead = 'img/masthead.jpg'
	private final static String _firstpage = 'firstpage.html'
	
	private def createHTMLPage(String path, String pagetitle, Closure pagecontent) {
		println "Creating HTML $path with title $pagetitle"
		def writer
		if(simulate) {
			writer = new StringWriter()	
		} else {
			//writer = new OutputStreamWriter(new FileOutputStream(output_path + path),'UTF-8')
			writer = new FileWriter(output_path + path)
		}
		def htmlb = new MarkupBuilder(writer)
		htmlb.mkp.xmlDeclaration('version':'1.0','encoding':'UTF-8')
		htmlb.mkp.yieldUnescaped('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">')
		htmlb.mkp.yield("\n")
		htmlb.html {
			head('xmlns':'http://www.w3.org/1999/xhtml') {
				meta('content':'text/html; charset=utf-8', 'http-equiv':'Content-Type')
				'title'(pagetitle)
			}
			
			body {
				h1(pagetitle)
				pagecontent.call(htmlb)
			}
		}
		
		if(simulate) {
			println writer.toString()
		} else { 
			writer.close()
		}
	}
		
	private def createFirstPage(covername, coverauthor, editionno) {
		def cfbc = { builder ->
			builder.div {
				img('src':_cover)
			}
			builder.div {
				font('size':'-1',coverauthor)
			}
		}
	
		createHTMLPage(_firstpage,"Esensja: $editionno", cfbc)
	}
	
	
	def parseEdition() {
		def page = parseURL('index.html')
	
		def covername = page.BODY.CENTER.TABLE.TBODY.TR[1].TD.A.IMG.'@src'[0]
		def coverauthor = page.BODY.CENTER.TABLE.TBODY.TR[1].TD.FONT.text()
		def editionno = page.BODY.CENTER.TABLE.TBODY.TR[0].TD.TABLE.TBODY.TR.TD[0].FONT[1..3].inject('') { str, item -> str + item.text() }
		saveFile(getFile(covername),_cover)
		createFirstPage(covername, coverauthor,editionno)
		
		def firstpage = page.BODY.CENTER.TABLE.TBODY.TR[1].TD.A.'@href'[0]
		page = parseURL(firstpage)
		
		def mastheadname = page.BODY.DIV.TABLE.TBODY.TR[1].TD.TABLE[1].TBODY.TR[1].TD.TABLE.TBODY.TR.TD.TABLE.TBODY.TR[0].TD[1].P.IMG.'@src'[0].substring(3)
		saveFile(getFile(mastheadname),_masthead)
		
		
	}
	
}

