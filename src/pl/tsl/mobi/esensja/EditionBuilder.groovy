package pl.tsl.mobi.esensja

class EditionBuilder {

	private static final String url_base = 'http://esensja.pl/magazyn/' 
	
	boolean parse_file = false
	boolean simulate = true
	
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
	
	private def parseURL(name) {
		if(parse_file){
		  return new XmlParser(parser).parse(new FileReader(url_path + name))
		} else {
		  return new XmlParser(parser).parse(url_path + name)
		}
	}
	
	private InputStream getFile(name) {
		if(parse_file) {
		  return new FileInputStream(url_path + name)
		} else {
		  return new URL(url_path + name).openStream()
		}
	}
	
	private def saveFile(InputStream what, name) {
		if(simulate) {
			println "Saving file: $name"
			return
		}
		def f = new File(output_path + name)
		if(f.exists()) {
			f.delete()
		} 
		f << what
	}
	
	def buildFiles() {
		def page = parseURL('index.html')
		
		def covername = page.BODY.CENTER.TABLE.TBODY.TR[1].TD.A.IMG.'@src'[0]
		saveFile(getFile(covername),'img/cover.jpg')
		
		def firstpage = page.BODY.CENTER.TABLE.TBODY.TR[1].TD.A.'@href'[0]
		println firstpage 
	}
}
