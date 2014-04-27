//strings
assert("length",s1.length("asdf")==4)
assert("contains",s1.contains("asdf.qwer.","."))
assert("contains2",!s1.contains("asdf.qwer.",","))
assert("startsWith",s1.contains("asdf.qwer.","as"))
assert("startsWith2",!s1.contains("asdf.qwer.",","))
assert("endsWith",s1.endsWith("asdf.qwer.","."))
assert("endsWith2",!s1.endsWith("asdf.qwer.",","))
assert("indexOf",s1.indexOf("asdf.qwer.",".")==4)
assert("indexOf2",s1.indexOf("asdf.qwer.",".")==4)
assert("indexOf3",s1.indexOf("asdf.qwer.",".")==4)
assert("indexOf-1",s1.indexOf("asdf.qwer",",")==-1)
assert("lastIndexOf",s1.lastIndexOf("asdf.qwer.",".")==9)
assert("lastIndexOf-1",s1.lastIndexOf("asdf.qwer.",",")==-1)
assert("substring",s1.substring("asdf.qwer.",0,4)=="asdf")
assert("substring2",s1.substring("asdf.qwer.",5,9)=="qwer")
assert("charAt",s1.charAt("asdf.qwer.",4)==".")
assert("toLowerCase",s1.toLowerCase("asDF")=="asdf")
assert("toUpperCase",s1.toUpperCase("asDF")=="ASDF")
assert("replace",s1.replace("asdf.qwer.zxcv.q",".q",".1")=="asdf.1wer.zxcv.1")
assert("replaceAll",s1.replaceAll("asdf.qwer.zxcv.q","\\.q",".1")=="asdf.1wer.zxcv.1")
assert("replaceFirst",s1.replaceFirst("asdf.qwer.zxcv.q","\\.q",".1")=="asdf.1wer.zxcv.q")
assert("matches",s1.matches("asdf.123.3","^[a-z]+\\.[0-9]+\\.3$"))
assert("matches2",!s1.matches("asdf.123a.3","^[a-z]+\\.[0-9]+\\.3$"))
assert("split",s1.length(s1.split("asdf.123a.3","\\."))==3)
print(s1.split("asdf.123a.3","\\.")[2])
assert("split2",s1.split("asdf.123a.3","\\.")[2]=="3")

//map
assert("diff",s1.length(s1.diff({a:1},{a:1,b:2}))==1)
assert("diff2",s1.diff({a:1},{a:1,b:2})[0].path=="b")
assert("diff3",s1.diff({a:1},{a:1,b:2})[0]['new']==2)
assert("diff4",s1.diff({a:1},{a:1,b:2})[0]['old']==null)
assert("length2",s1.length({a:1,b:2})==2)
assert("merge",s1.merge([{},{a:1,c:3},{a:2,b:1}]).a==2)
assert("merge2",s1.merge([{},{a:1,c:3},{a:2,b:1}]).b==1)
assert("merge3",s1.merge([{},{a:1,c:3},{a:2,b:1}]).c==3)
assert("iterate",s1.iterate({a:1,b:"2"},function(path,value,name){
    if(name=="b")
        return "3"
    return value;
}).a==1)
assert("iterate2",s1.iterate({a:1,b:"2"},function(path,value,name){
    if(name=="b")
        return "3"
    return value;
}).b=="3")

//list
assert("length3",s1.length([{a:1,b:2},2])==2)
var arr = [1,2,3];
s1.add(arr,4);
assert("add",s1.length(arr)==4)
assert("arr3",arr[3]==4)
s1.addAll(arr,[5,6,7]);
assert("addAll",s1.length(arr)==7)
s1.remove(arr,s1.length(arr)-1)
s1.remove(arr,s1.length(arr)-1)
print(arr);
assert("remove",s1.length(arr)==5)

//user
assert("whoAmI",s1.whoAmI().id=='anonymous');
assert("inRole",!s1.inRole('test'));