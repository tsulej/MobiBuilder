package pl.tsl.mobi.esensja

import groovy.xml.MarkupBuilder

class EditionBuilder {

	private static final String url_base = 'http://esensja.pl/magazyn/' 
	
	boolean parse_file = false
	boolean simulate = true
	
	String edition_year
	String edition_month
	private String url_path 
	private output_path
	
	def content = [
		'html' : [],
		'sections' : [],
		'imgs' : [:],
		'mbpmeta' : [:]
	]
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
	
	
	private def createTOCInfo(xml) {
		// table of contents
		def toc_info = [:]
		// order of chapters
		def toc = []
		def current_chapter = 'Wstęp'
		def current_subchapter = ''
		def current_chapter_name = { a,b ->
			if(b) {
			  return "$a - $b"
			} else {
			  return a
			}
		}.memoize()		
		
		xml.each { tr ->
			def type = tr.TD.'@class'[0]
			switch(type) {
				case 'position':
					def title_tag = tr.TD.DIV.grep { it.'@class' == 'n-title' }
					def art_url = title_tag[0].A.'@href'[0]
					def art_title = title_tag[0].A.text()
					def author_name = tr.TD.DIV.grep { it.'@class' == 'n-author' }[0].text()
					def art_descr = tr.TD.DIV.grep { it.'@class' == 'subtitle' }[0].text()
					def section = current_chapter_name(current_chapter, current_subchapter)

					def ttt = toc_info[section]
					if(!ttt) {
						ttt = []
					}
					toc_info[section] = ttt << [art_url, art_title, author_name, art_descr]
					if(!toc.contains(section)) {
						toc += section
					}
					break
				
				case 'chapter':
					current_chapter = tr.TD.text()
					break
					
				case 'subchapter':
					current_subchapter = tr.TD.text()
					break
			}
		}
		
		toc_info['_TOC_'] = toc
		return toc_info
	}
	
	def parseEdition() {
		def page = parseURL('index.html')
		
		//cover and cover page
		def covername = page.BODY.CENTER.TABLE.TBODY.TR[1].TD.A.IMG.'@src'[0]
		def coverauthor = page.BODY.CENTER.TABLE.TBODY.TR[1].TD.FONT.text()
		def editionno = page.BODY.CENTER.TABLE.TBODY.TR[0].TD.TABLE.TBODY.TR.TD[0].FONT[1..3].inject('') { str, item -> str + item.text() }
		saveFile(getFile(covername),_cover)
		createFirstPage(covername, coverauthor,editionno)
		
		// parse first page, get masthead and toc
		def firstpage = page.BODY.CENTER.TABLE.TBODY.TR[1].TD.A.'@href'[0]
		page = parseURL(firstpage)
		def mastheadname = page.BODY.DIV.TABLE.TBODY.TR[1].TD.TABLE[1].TBODY.TR[1].TD.TABLE.TBODY.TR.TD.TABLE.TBODY.TR[0].TD[1].P.IMG.'@src'[0].substring(3)
		saveFile(getFile(mastheadname),_masthead)
		
		def toc_tab = page.BODY.DIV.TABLE.TBODY.TR[1].TD.TABLE[1].TBODY.TR[1].TD.TABLE.TBODY.TR.TD.TABLE.TBODY.TR[2].TD.TABLE.TBODY.TR.TD[0].TABLE.TBODY.TR + page.BODY.DIV.TABLE.TBODY.TR[1].TD.TABLE[1].TBODY.TR[1].TD.TABLE.TBODY.TR.TD.TABLE.TBODY.TR[2].TD.TABLE.TBODY.TR.TD[1].TABLE.TBODY.TR
	
		//iterate through toc
		def toc_info = createTOCInfo(toc_tab)
		def chapter_order = toc_info.remove('_TOC_')
		
		def getArtName = { url ->
			return 'h' + url.findAll(/[0-9]/).join('')
		}
		
		boolean first = true
		chapter_order.each { section ->
			def art_list = []
			
			if(first) {
				art_list += 'firstpage'
				content['html'] <<= ['firstpage','firstpage.html','Okładka']
				content['mbpmeta']['firstpage'] = ['Esensja','Okładka']
				
				first = false
			}
			
			//[art_url, art_title, author_name, art_descr]
			toc_info[section].each { art_url, art_title, author_name, art_descr ->
				def html_id = getArtName(art_url)
				//parse art URL and get images
				def imgs = parseArticle(art_url, html_id)
				content['imgs'] += imgs
				
				content['html'] <<= [html_id,"${html_id}.html",art_title]
				content['mbpmeta'][html_id] = [author_name,art_descr]
 				art_list += html_id
			}
			
			// teraz lecimy po kolei artykuły, parsujemy, zapisujemy uproszczenie i uzupełniamy content
			
			content['sections'] <<= [section,art_list]
		}	
		
		
		// TODO: add firstpage as a first article in first section!
		return content
	}
	
	//produce html and get all necessary images
	private def parseArticle(art_url, html_id) {
		def imgs_tab = []
		
		return imgs_tab
	}
	
}

