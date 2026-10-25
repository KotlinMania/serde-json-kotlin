from pathlib import Path
import argparse,json,hashlib,subprocess,difflib
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1];GEN=ROOT.parent/'rust-kotlin/verification/foundation-batch-04/errors-05'
p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
report={'status':'running','commands':[]}
def run(label,argv,input=None):
 argv=list(map(str,argv));r=subprocess.run(argv,cwd=ROOT,capture_output=True,input=input);(out/(label+'.stdout')).write_bytes(r.stdout);(out/(label+'.stderr')).write_bytes(r.stderr);report['commands'].append(dict(label=label,argv=argv,exit=r.returncode));assert r.returncode==0,(label,r.stderr.decode()[-4000:]);return r.stdout
try:
 raw=GEN/'generated/generated.kt';installed=ROOT/'src/commonMain/kotlin/io/github/kotlinmania/serdejson/lexical/generated/errors/Errors.kt'
 assert installed.read_bytes().endswith(raw.read_bytes())
 report['generated']={'raw_sha256':sha(raw),'installed':str(installed),'installed_sha256':sha(installed),'raw_body_unchanged':True}
 klib=ROOT/'build/classes/kotlin/macosArm64/main/klib/serde-json-kotlin'
 deps=[Path(l.removeprefix('NATIVE_INPUT=')) for l in (ROOT/'verification/generated-shift-01/native-inputs.txt').read_text().splitlines() if l.startswith('NATIVE_INPUT=')]
 assert klib.exists() and deps
 report['production_klib']={str(p.relative_to(klib)):sha(p) for p in klib.rglob('*') if p.is_file()};report['dependencies']={str(p):sha(p) for p in deps}
 original=(GEN/'Oracle.kt').read_text()
 raw_consumer='import io.github.kotlinmania.serdejson.lexical.generated.errors.*\n'+original
 legacy=original.replace('import io.github.kotlinmania.serdejson.lexical.F32Float','import io.github.kotlinmania.serdejson.lexical.*').replace('__rtkImpl0.error_scale()','ULongFloatErrors.errorScale()').replace('__rtkImpl0.error_halfscale()','ULongFloatErrors.errorHalfscale()').replace('__rtkImpl0.error_is_accurate(count,fp,if(width==32) F32Float else F64Float)','errorIsAccurate(if(width==32) F32Float else F64Float,count,fp)')
 expected=(GEN/'rust.stdout').read_bytes();cases=(GEN/'cases.txt').read_bytes()
 compiler='/Users/sydney/.konan/kotlin-native-prebuilt-macos-aarch64-2.4.21/bin/konanc'
 for label,source in [('raw-production',raw_consumer),('adapter-production',legacy)]:
  path=out/(label+'.kt');path.write_text(source)
  run(label+'-compile',[compiler,'-target','macos_arm64','-Werror','-Xoverride-konan-properties=airplaneMode=true','-Xbackend-threads=1',path,'-friend-modules',klib,'-library',klib,*[x for p in deps for x in ['-library',p]],'-o',out/label])
  data=run(label,[out/(label+'.kexe')],cases);(out/(label+'.diff')).write_text(''.join(difflib.unified_diff(expected.decode().splitlines(True),data.decode().splitlines(True))))
  assert data==expected,label;report[label]={'observations':len(data.splitlines()),'matches_rust':True}
 assert installed.read_bytes().endswith(raw.read_bytes())
 report.update(status='passed',target='macos_arm64',adopted_whole_files=1)
 report['boundaries']=['Existing entry points forward the explicit FloatTraits provider; original f32/f64 providers verified','Scalar snapshot adapter preserves shared input values; callers must not concurrently mutate the shared state','Overflow/debug checks on, panic unwind, usize64; masks use existing production helpers, checked against original Rust','No allocation, full dependency translation, Swift ABI or other-platform runtime equivalence claim']
except Exception as e:report.update(status='failed',error=str(e));raise
finally:(out/'validation.json').write_text(json.dumps(report,indent=2)+'\n')
print(report['status'],report['raw-production'],report['adapter-production'])
