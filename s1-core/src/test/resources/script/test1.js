var x = {a:1,b:true,y:null,z:['1','2',{a:'eee'}],m:{a:222,m1:{a:2}}};
x.m.m1.c = "qwe123";
var x1 = x.m.m1.a;
var x2 = x.z[2].a;

var fn1 = null;

if(1==1){
    var str1="qwer";
    fn1 = function(a){
        return a+str1;
    }
}
var fn1_res = fn1("asdf_");

var ext_sum_res = ext_sum(3,4);

var a = function(s){
    var z=1;
    if(s<10)
        z+=a(s+1);
    //
    return z;
};
x.m.m1.c+="_a";
if(x.m.m1.c){
    //x.m.m1.c=null;
    delete x.m.m1.c;
}
if(x.m.m1.c){
    x.m.m1.a = ((3+7)*2+8)/7;
}else{
    x.m.m1.a = ((3+7)*2+1)/7;
}
if(!x.m.m1.c){

}
if(x.m.m1.c==null){
    x.m.m1.c="yyy";
}
if(x.m.m1.c!=null){
    x.m.m1.a+=2;
}
if(x.m.m1.a>0){
    x.m.m1.a = x.m.m1.a/2;
}
//qwer
var y=1;
for(var i=0;i<3;i++){
    y+=1;
}

for(var i in x.m.m1){
    x.z[0]+="_"+i;
}


try{
y=2;
if(y==2)
    throw "test";
y=3;
}catch(e if e=='test'){
    y = 'qwer';
}catch(e){
    y = e;
}finally{

}
var zzz = null;
try{
zzz = a(0);
}catch(e){
}finally{
}