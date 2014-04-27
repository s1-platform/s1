var x = {a:1,b:true,y:null,z:['1','2',{a:'eee'}],m:{a:222,m1:{a:2}}};
x.m.m1.c = "qwe123";

assert("1",x.m.m1.c=="qwe123");
assert("2",x.z[2].a=="eee");
assert("3",x.z[1]=="2");

assert("t1",x.z[4]==null)
assert("t2",(x.z[4]?"a":"b")=="b")
assert("t2",(x.z[2].a?"a":"b")=="a")

assert("4_0",s1.get(x,'a', null)==1.0);
assert("4",s1.get(x,'m1.m1.c', null)==null);
assert("5",s1.get(x,'m1.m2.c1',"test")=="test");
assert("6",s1.get(x,'z[10].a',"qwer")=="qwer");

print(""+x.z)

s1.set(x,'z[2].a',"qwer1234");
print(s1.get(x,'z[2].a',null))
assert("7",s1.get(x,'z[2].a',null)=="qwer1234");