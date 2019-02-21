package wvdroid

type Bridge interface {
	Eval(js string)
}

var bridgeMap = make(map[string]Bridge)

func Init(b Bridge, base string) {
	bridgeMap[base] = b
}

//return shouldOverrideUrlLoading
//main entry url is file://android_asset/index.html
func OnLoadURL(url, base string) bool {
	return false
}
