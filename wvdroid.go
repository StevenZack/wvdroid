package wvdroid

type Bridge interface {
	Eval(js string)
}

var bridge Bridge

func Init(b Bridge,base string) {
	bridge = b
}

//return shouldOverrideUrlLoading
//main entry url is file://android_assets/index.html
func OnLoadURL(url,base string) bool {
	return false
}
