from pathlib import Path
import argparse,json,hashlib,subprocess,difflib
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1];GEN=ROOT.parent/'rust-kotlin/verification/foundation-batch-04/cached-03'
p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
report={'status':'running','commands':[]}
def run(label,argv):
 argv=list(map(str,argv));r=subprocess.run(argv,cwd=ROOT,capture_output=True);(out/(label+'.stdout')).write_bytes(r.stdout);(out/(label+'.stderr')).write_bytes(r.stderr);report['commands'].append(dict(label=label,argv=argv,exit=r.returncode));assert r.returncode==0,(label,r.stderr.decode());return r.stdout
try:
 raw=GEN/'generated/generated.kt';installed=ROOT/'src/commonMain/kotlin/io/github/kotlinmania/serdejson/lexical/generated/cached/Cached.kt'
 assert installed.read_bytes().endswith(raw.read_bytes())
 report['generated']={'raw_sha256':sha(raw),'installed':str(installed),'installed_sha256':sha(installed),'raw_body_unchanged':True}
 klib=ROOT/'build/classes/kotlin/macosArm64/main/klib/serde-json-kotlin'
 deps=[Path(l.removeprefix('NATIVE_INPUT=')) for l in (ROOT/'verification/generated-shift-01/native-inputs.txt').read_text().splitlines() if l.startswith('NATIVE_INPUT=')]
 assert klib.exists() and deps
 report['production_klib']={str(p.relative_to(klib)):sha(p) for p in klib.rglob('*') if p.is_file()};report['dependencies']={str(p):sha(p) for p in deps}
 original=(GEN/'Oracle.kt').read_text()
 raw_consumer='import io.github.kotlinmania.serdejson.lexical.generated.cached.*\n'+original
 legacy=original.replace('__rtkImpl0.get_powers()','ExtendedFloatCache.getPowers()').replace('.len()', '.length.toULong()').replace('.small_int','.smallInt')
 for old,new in [('powers.get_small(index)','powers.getSmall(toLegacyIndex(index))'),('powers.get_large(index)','powers.getLarge(toLegacyIndex(index))'),('powers.get_small_int(index)','powers.getSmallInt(toLegacyIndex(index))'),('values.get_extended_float(index)','values.getExtendedFloat(toLegacyIndex(index))'),('ExtendedFloatArray(mant,exp)','ExtendedFloatArray(mant.toULongArray(),exp.toIntArray())')]:legacy=legacy.replace(old,new)
 legacy=legacy.replace('    val result=try','    if (!legacyIndex(index)) return\n    val result=try',1).replace('    for(index in indices) {','    for(index in indices) {\n        if (!legacyIndex(index)) continue')
 legacy='''@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
import io.github.kotlinmania.serdejson.lexical.*
import io.github.kotlinmania.serdejson.lexical.generated.cached.__rtkPanic
private fun legacyIndex(index:ULong) = index <= Int.MAX_VALUE.toULong() || index == ULong.MAX_VALUE
private fun toLegacyIndex(index:ULong):Int { require(legacyIndex(index)); return index.toInt() }
'''+legacy
 expected=(GEN/'rust.stdout').read_bytes()
 legacy_expected=b''.join(line for line in expected.splitlines(True) if not (line.split(b'|')[0] in [b'small',b'large',b'int',b'array'] and int(line.split(b'|')[1])>2147483647 and int(line.split(b'|')[1])!=18446744073709551615))
 compiler='/Users/sydney/.konan/kotlin-native-prebuilt-macos-aarch64-2.4.21/bin/konanc'
 for label,source,gold in [('raw-production',raw_consumer,expected),('legacy-production',legacy,legacy_expected)]:
  path=out/(label+'.kt');path.write_text(source)
  run(label+'-compile',[compiler,'-target','macos_arm64','-Werror','-Xoverride-konan-properties=airplaneMode=true','-Xbackend-threads=1',path,'-friend-modules',klib,'-library',klib,*[x for p in deps for x in ['-library',p]],'-o',out/label])
  data=run(label,[out/(label+'.kexe')]);(out/(label+'.diff')).write_text(''.join(difflib.unified_diff(gold.decode().splitlines(True),data.decode().splitlines(True))))
  assert data==gold,label;report[label]={'observations':len(data.splitlines()),'matches_rust':True}
 assert installed.read_bytes().endswith(raw.read_bytes())
 report.update(status='passed',target='macos_arm64',adopted_whole_files=1)
 report['boundaries']=['Raw API checks all usize64 cases against Rust; Int adapter checks representable indices plus -1 mapped to usize::MAX','Primitive-array adapter retains read-only list views; backing cached arrays must remain immutable for the view lifetime','Explicit static binding reuses generated cached_float80 values; no new table or handwritten lookup implementation','No all-target runtime, layout/allocation or Swift ABI equivalence claim']
except Exception as e:report.update(status='failed',error=str(e));raise
finally:(out/'validation.json').write_text(json.dumps(report,indent=2)+'\n')
print(report['status'],report['raw-production'],report['legacy-production'])
