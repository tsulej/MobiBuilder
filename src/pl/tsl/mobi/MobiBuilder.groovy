package pl.tsl.mobi

import groovy.xml.MarkupBuilder

class MobiBuilder {

	private String bookname
	boolean periodical = false
	
	String cover_name = 'cover.jpg' // max 600x800
	String masthead_name = 'masthead.gif' // max 600x100
	String title = '[title not set]'
	String language = 'en-US'
	String creator = '[creator not set]'
	String publisher = '[publisher not set]'
	String subject = '[subject not set]'
	String date = '[date not set]' // format YYYY-MM-DD or YYYY
	String description = '[description not set]'
	String identifier = UUID.randomUUID().toString()
	
	String base_path = ''
	
	static final String toc_name = 'toc.html'
	static final String ncx_name = 'toc.ncx'
	static final def exts = ['gif' : 'image/gif', 
							 'html' : 'application/xhtml+xml',
							 'jpg' : 'image/jpeg',
							 'ncx' : 'application/x-dtbncx+xml'] 
	private String opf_name
	
	
	/*
	 * Content structure is a map with following content
	 *  - 'html' - array of html files, each value is an array [id, value, title]
	 *  - 'sections' - map of sections, key: section name, value: html ids list, there are no subsections in TOC
	 *  - 'imgs' - map of images files, key: id, value: filename
	 *  
	 */
	def content
	
	MobiBuilder(String bookname) {
		this.bookname = bookname
		this.opf_name = "${bookname}.opf" 
	}
	
	private def getContentType(filename) {
		def ext = filename.tokenize('.')[1]
		return exts[ext]
	}
	
	private def getItem(xml, id, val) {
		xml.item('id':id, 'href':val, 'media-type':getContentType(val))
	}
	
	def generateOPF() {
		def opf_namespace = 'http://www.idpf.org/2007/opf'
		def dc_namespace = 'http://purl.org/dc/elements/1.1/'
		def writer = new FileWriter(base_path + opf_name)
		def xml = new MarkupBuilder(writer)
		xml.mkp.xmlDeclaration('version':'1.0','encoding':'UTF-8')
		xml.package(xmlns:opf_namespace, 'version':'2.0', 'unique-identifier':'book_id') {
			
			metadata('xmlns:dc':dc_namespace, 'xmlns:opf':opf_namespace) {
			  'dc:title'(title)
			  'dc:language'(language)
			  'dc:creator'(creator)
			  'dc:publisher'(publisher)
			  'dc:subject'(subject)
			  'dc:date'(date)
			  'dc:description'(description)
			  'dc:identifier'('id':'book_id',identifier)
			  meta('name':'cover', 'content':'book_cover')
			  if(periodical) {
				  'x-metadata' {
					  output('content-type':'application/x-mobipocket-subscription-magazine','encoding':'utf-8')
				  }
			  }
			}
			
			manifest {
				content.html.each {
					getItem(xml,it[0],it[1])
				}
				content.imgs.each { 
					getItem(xml,it.key,it.value)
				}
				getItem(xml,'toc_ncx',ncx_name)
				getItem(xml,'book_cover', cover_name)
				getItem(xml,'toc_html',toc_name)
				getItem(xml,'masthead',masthead_name)
			}
			
			spine('toc':'toc_ncx') {
				itemref('idref':'toc_html')
				content.html.each {
					itemref('idref':it[0])
				}			
			}
			
			guide {
				reference('type':'toc','title':'Table of Contents','href':toc_name)
				reference('type':'text','title':content.html[0][2],'href':content.html[0][1])
			}
		}
		return writer.close()
	}
	
	private def getSourcebyId(id) {
		def t = content.html.find { it[0] == id }
		return t
	}
	
	private def getNavPoints(xml) {
		def order_id = 1
		content.sections.each { s_title,s_cont ->
			xml.navPoint('playOrder':order_id,'class':'section','id':"section_${order_id}") {
				navLabel { text(s_title) }
				content('src' : getSourcebyId(s_cont[0])[1]) // first article as a section article
				order_id++
				s_cont.each { a_id ->
					navPoint('playOrder':order_id,'class':'article','id':"doc_${a_id}") {
						navLabel { text( getSourcebyId(a_id)[2]) }
						content('src' : getSourcebyId(a_id)[1])
						//mpb:meta tags for article list in periodicals
						//'mbp:meta'('name':'description','description here')
						//'mbp:meta'('name':'author','authors here')
												
						order_id++
					}
				}
			}
			
		}
	}
	
	def generateNCX() {
		def ncx_namespace = 'http://www.daisy.org/z3986/2005/ncx/'
		def mbp_namespace = 'http://mobipocket.com/ns/mbp'
		//def writer = new OutputStreamWriter(new FileOutputStream(base_path + ncx_name),'UTF-8')
		def writer = new FileWriter(base_path + ncx_name)
		def xml = new MarkupBuilder(writer)
		xml.mkp.xmlDeclaration('version':'1.0','encoding':'UTF-8')
		xml.mkp.yieldUnescaped('<!DOCTYPE ncx PUBLIC "-//NISO//DTD ncx 2005-1//EN" "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd">')
		xml.mkp.yield("\n")
		xml.ncx('xmlns:mbp':mbp_namespace,'xmlns':ncx_namespace, 'version':'2005-1','xml:lang':language) {
			
			head {
				meta('name':'dtb:uid', 'content':'book_id')
				meta('name':'dtb:depth', 'content':'2')
				meta('name':'dtb:totalPageCount', 'content':'0')
				meta('name':'dtb:maxPageNumber', 'content':'0')
			}
			
			docTitle { text(title) }
			docAuthor { text(creator) }
			
			
			navMap {
				if(periodical) {
					navPoint('playOrder':'0','class':'periodical','id':'periodical') {
						'mbp:meta-img'('src':masthead_name,'name':'mastheadImage','id':'masthead')
						navLabel {
							text('Table of Contents')
						}
						content('src':toc_name)
						getNavPoints(xml)
					}
				} else {
					getNavPoints(xml)
				}
			}
		}
		writer.close()
	}
	
	def generateTOC() {
		def writer = new OutputStreamWriter(new FileOutputStream(base_path + toc_name),'UTF-8')
		def htmlb = new MarkupBuilder(writer)
		htmlb.mkp.xmlDeclaration('version':'1.0','encoding':'UTF-8')
		htmlb.mkp.yieldUnescaped('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">')
		htmlb.mkp.yield("\n")
		htmlb.html {
			head('xmlns':'http://www.w3.org/1999/xhtml') {
				meta('content':'text/html; charset=utf-8', 'http-equiv':'Content-Type')
				'title'('Table of Contents')
			}
			
			body {
				h1('Zawartość')
				content.sections.each { s_title, s_cont ->
					h4(s_title)
					ul {
						s_cont.each { a_id ->
							li {
								a('href':getSourcebyId(a_id)[1],getSourcebyId(a_id)[2]) 
							}
						}
					}
				}
			}
		}
		writer.close()
	}
	
	def generateBook() {
		println 'Generating mobi book:'
		println this
		print 'Generating NCX...'
		generateNCX()
		println 'done.'
		print 'Generating OPF...'
		generateOPF()
		println 'done.'
		print 'Generating TOC...'
		generateTOC()
		println 'done.'
	}
	
	String toString() {
		return """
Bookname: $bookname
Periodical issue? $periodical
Title: $title
Language: $language
Author: $creator
Publisher: $publisher
Subject: $subject
Date: $date
Description: $description
ID: $identifier

"""	
	}
	
}

