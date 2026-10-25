from pathlib import Path
import argparse,json,hashlib,subprocess,difflib
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
GEN=ROOT.parent/'rust-kotlin/verification/foundation-batch-04'
p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
report={'status':'running','commands':[],'new_adoptions':0}
def run(label,argv):
    argv=list(map(str,argv));r=subprocess.run(argv,cwd=ROOT,capture_output=True)
    (out/(label+'.stdout')).write_bytes(r.stdout);(out/(label+'.stderr')).write_bytes(r.stderr)
    report['commands'].append(dict(label=label,argv=argv,exit=r.returncode))
    assert r.returncode==0,(label,r.stderr.decode()[-5000:])
    return r.stdout
try:
    raw=GEN/'rounding-06/generated/generated.kt'
    installed=ROOT/'src/commonMain/kotlin/io/github/kotlinmania/serdejson/lexical/generated/rounding/Rounding.kt'
    adapter=ROOT/'src/commonMain/kotlin/io/github/kotlinmania/serdejson/lexical/Rounding.kt'
    assert installed.read_bytes().endswith(raw.read_bytes())
    report['generated']=dict(raw_sha256=sha(raw),installed_sha256=sha(installed),adapter_sha256=sha(adapter),raw_body_unchanged=True)
    klib=ROOT/'build/classes/kotlin/macosArm64/main/klib/serde-json-kotlin'
    deps=[Path(l.removeprefix('NATIVE_INPUT=')) for l in (ROOT/'verification/generated-shift-01/native-inputs.txt').read_text().splitlines() if l.startswith('NATIVE_INPUT=')]
    assert klib.exists() and deps
    report['production_klib']={str(p.relative_to(klib)):sha(p) for p in klib.rglob('*') if p.is_file()}
    report['dependencies']={str(p):sha(p) for p in deps}
    original=(GEN/'concrete-methods-01/Oracle.kt').read_text()
    raw_consumer='import io.github.kotlinmania.serdejson.lexical.generated.rounding.*\n'+original
    legacy='import io.github.kotlinmania.serdejson.lexical.ExtendedFloat\n'+original
    for old,new in [('round_nearest_tie_even','roundNearestTieEven'),('round_nearest','roundNearest'),('round_downward','roundDownward'),('internal_n_mask','internalNMask'),('lower_n_halfway','lowerNHalfway'),('lower_n_mask','lowerNMask'),('nth_bit','nthBit'),('tie_even','tieEven')]:
        legacy=legacy.replace(old+'(',new+'(')
        legacy='import io.github.kotlinmania.serdejson.lexical.'+new+'\n'+legacy
    for old,new in [('round_to_float','roundToFloat'),('round_to_native','roundToNative')]:
        legacy=legacy.replace(old+'(fp, callback, provider)',new+'(fp, provider, callback)')
        legacy='import io.github.kotlinmania.serdejson.lexical.'+new+'\n'+legacy
    legacy=legacy.replace('avoid_overflow(', 'avoidOverflow(')
    legacy='import io.github.kotlinmania.serdejson.lexical.avoidOverflow\n'+legacy
    expected=(GEN/'rounding-02/rust.stdout').read_bytes()
    compiler='/Users/sydney/.konan/kotlin-native-prebuilt-macos-aarch64-2.4.21/bin/konanc'
    options=['-target','macos_arm64','-Werror','-Xoverride-konan-properties=airplaneMode=true','-Xbackend-threads=1','-friend-modules',klib,'-library',klib,*[x for p in deps for x in ['-library',p]]]
    def compare(label,source,expected):
        path=out/(label+'.kt');path.write_text(source)
        run(label+'-compile',[compiler,*options,path,'-o',out/label])
        data=run(label,[out/(label+'.kexe')])
        (out/(label+'.diff')).write_text(''.join(difflib.unified_diff(expected.decode().splitlines(True),data.decode().splitlines(True))))
        assert data==expected,label
        report[label]=dict(observations=len(data.splitlines()),matches_rust=True)
    compare('raw-production',raw_consumer,expected)
    compare('adapter-production',legacy,expected)
    # Same frozen cases, adding the callback-entry state observation required for the adapter.
    # Original baseline files remain unchanged; this is not additional file credit.
    rust=(GEN/'rounding-02/Oracle.rs').read_text()
    rust=rust.replace('let mut observed = None;', 'let mut observed = None;\n                    let mut callback_state = None;')
    rust=rust.replace('observed = Some(shift);','observed = Some(shift);\n                            callback_state = Some((value.mant, value.exp));')
    rust=rust.replace('|{calls}|{:?}",','|{calls}|{:?}|{:?}",').replace('fp.mant, fp.exp, observed);','fp.mant, fp.exp, observed, callback_state);')
    rust_path=out/'CallbackOracle.rs';rust_path.write_text(rust)
    rust_compiler=json.loads((GEN/'shift-01/rust-validation.json').read_text())['compiler']
    run('callback-rust-compile',[rust_compiler,'--edition=2021','--cfg','feature="std"','--cfg','feature="alloc"','--cfg','fast_arithmetic="64"','-C','overflow-checks=on','-C','debug-assertions=on','-C','panic=unwind',rust_path,'-o',out/'callback-rust'])
    traced=run('callback-rust',[out/'callback-rust'])
    stripped=b'\n'.join(row.rsplit(b'|',1)[0] if row.startswith(b'generic|') else row for row in traced.splitlines())+b'\n'
    assert stripped==expected,'callback tracing changed original observations'
    trace=legacy.replace('var observed: Int? = null','var observed: Int? = null\n            var callbackState: String? = null')
    trace=trace.replace('observed = shift','observed = shift\n                check(value === fp)\n                callbackState = "(${value.mant}, ${value.exp})"')
    trace=trace.replace('?: "None"}")','?: "None"}|${callbackState?.let { "Some($it)" } ?: "None"}")')
    compare('callback-adapter',trace,traced)
    assert installed.read_bytes().endswith(raw.read_bytes())
    report.update(status='passed',target='macos_arm64',new_adoptions=1)
    report['boundaries']=['Complete raw production and existing adapter API match canonical checked usize64 Rust behavior','Same 3852 cases separately compare callback-entry scalar state; adapter callbacks receive the original Kotlin object','Callback calls, arguments, mutations and exceptional copyback preserved; actual f32/f64 providers','No allocation, custom Drop, escaping loans, Swift ABI or other-target runtime equivalence claim']
except Exception as e:report.update(status='failed',error=str(e));raise
finally:(out/'validation.json').write_text(json.dumps(report,indent=2)+'\n')
print(report['status'],report['raw-production'],report['adapter-production'],report['callback-adapter'])
