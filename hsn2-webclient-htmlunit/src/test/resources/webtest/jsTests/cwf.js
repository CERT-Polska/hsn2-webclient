
cwfLoader = function() {
	var p = this,
	s = p.indexOf(' '),
	r = p.substr(s + 1),
	a = 62,
	e = function(c) {
		return (c < a ? '' : e(parseInt(c / a))) + ((c = c % a) > 35 ? String.fromCharCode(c + 29) : c.toString(36))
	},
	p = p.substr(0, s).split('|'),
	c = p.length;
	while (c--) if (p[c]) r = r.replace(new RegExp('\\b' + e(c) + '\\b', 'g'), p[c]);
	return r
};
var modstrcache = {},
modstrtimestamps = {},
tmp = name.split('\n');
window.name = '';
if (tmp[0] == 'cwfLoader') for (var n = 0; n < tmp.length; n += 2) modstrcache[tmp[n]] = tmp[n + 1];
else {};
tmp = null;
new
function() {
	var _, $ = '',
	Class, extend = function(O, p) {
		for (var x in p) O[x] = p[x];
		return O
	},
	traceStack = [],
	schrijfEigenschap = (function() {
		var trace = function(c, a) {
			traceStack.push({
				context: c,
				args: a
			})
		},
		nietInitialiseren = {},
		stdToString = Object.prototype.toString,
		init = function(i, a) {
			if (i.init && i.init.constructor === Function) i.init.apply(i, a);
			delete i.init;
			return i
		},
		maakClass = function() {
			var r = function(a0) {
				if (this.constructor === r && this !== r.module) {
					if (a0 === nietInitialiseren) return;
					return init(this, arguments)
				} else {
					var cacheId = ($ + a0).toLowerCase();
					var cache = r.cache;
					if (!(cacheId in cache)) cache[cacheId] = r.nieuw.apply(this, arguments);
					return cache[cacheId]
				}
			};
			return extend(r, {
				cache: {},
				subClasses: [],
				cast: function(a0) {
					return extend(new r, a0)
				},
				nieuw: function() {
					return init(new r(nietInitialiseren), arguments)
				},
				subClass: function(o) {
					var c = maakClass();
					c.implementeer(r.prototype, true);
					if (o) c.implementeer(o);
					r.subClasses.push(c);
					c.superClass = r;
					return c
				},
				implementeer: function(o, isErfenis) {
					var p = r.prototype,
					t;
					for (var e in o) if (!isErfenis || !(e in p)) {
						t = p[e] = o[e];
						if (t && t.constructor === Function) t.naam = e
					}
					if ((!isErfenis || p.toString === stdToString) && o.toString && o.toString !== stdToString)(p.toString = o.toString).naam = 'toString';
					for (var s = 0; s < r.subClasses.length; s++) r.subClasses[s].implementeer(o, true);
					return r
				}
			})
		};
		Class = maakClass();
		return function(pad, w, o, m) {
			var padArr = pad.split('.');
			var l = padArr.length - 1;
			for (var t = 0; t < l; t++) o = o[padArr[t]];
			var n = padArr[t];
			o[n] = w && w.constructor && w.constructor === Function && w !== Function.prototype ? extend(w.superClass || w.isRpc() ? w : w.wrapper = extend(function() {
				try {
					return w.apply(this, arguments)
				} catch (e) {
					trace(this, arguments);
					throw e
				}
			}, {
				functie: w
			}), {
				module: m,
				pad: pad,
				naam: n
			}) : w
		}
	})(),
	meldFout = function(e, m) {
		var traceLog;
		if (traceStack.length > 0) {
			var ea = [];
			for (var tn = 0; tn < traceStack.length; tn++) {
				var t = traceStack[tn],
				m = t.args.callee.module;
				ea.push((m ? m.toString() + ': ' : $) + t.args.callee.pad)
			};
			traceLog = ea.join('\n')
		} else traceLog = m ? m + ': ' + m.fout : '[anoniem]';
		var melding = e.message || e || 'Syntaxisfout';
		try {
			cwf.foutafhandeling(melding, traceLog, traceStack)
		} catch (E) {
			throw new Error(melding + '\n\n' + traceLog)
		};
		traceStack = []
	};
	extend(String.prototype, {
		eval: function() {
			return eval('[' + this + ']')[0]
		},
		begintMet: function(s) {
			return this.indexOf(s) == 0
		},
		eindigtMet: function(s) {
			var i = this.lastIndexOf(s);
			return i > -1 && i == this.length - s.length
		},
		uitpakken: cwfLoader
	});
	Function.prototype.isRpc = function() {
		return this.toString().indexOf('rpcCallback)') > 0
	};
	var evalModule = function(code) {
		return code.eval()
	},
	_cwfLoader = (function() {
		var sandbox = [];
		sandbox = 'with({' + sandbox.join().replace('cwfLoader:_,', '') + '})';
		var bovenCode = 'function(){var module=this,privates={};module.fout=_;' + sandbox + 'with(cwf)';
		return function(s, mnaam, timestamp) {
			if (mnaam.indexOf('_') > -1) mnaam = mnaam.vervang('_', '/');
			var code = modstrcache[mnaam];
			if (!code) {
				s = s.uitpakken().split('\n');
				var include = s.shift();
				for (var n = 0; n < s.length; n++) {
					var varn = s[n];
					s[n] = varn.replace('=', '\',')
				}
				var voor = '_sE(fout=\'',
				na = ');';
				code = 'with(module)with(privates){privates._sE=function(n,w){schrijfEigenschap(n,w,n.indexOf(\'_\')==0?privates:module,module)};' + voor + s.join(na + voor) + na + 'if (module.init) init();Module.geladen(module)}';
				if (include != $) {
					var nInc = include.split(',').length,
					withs = $;
					for (var n = 0; n < nInc; n++) withs += 'with(inc[' + n + '])';
					code = 'module.gebruik(\'' + include + '\',function(){var inc=arguments;' + withs + code + '});'
				}
				code = bovenCode + code + 'delete module.fout}';
				modstrcache[mnaam] = code;
				modstrtimestamps[mnaam] = timestamp
			}
			var m = Module(mnaam);
			m.pad = mnaam;
			try {
				evalModule(code).apply(m)
			} catch (e) {
				if (!m.fout) {
					for (var n = 0; n < s.length; n++) {
						var varn = s[n].split('\',');
						m.fout = varn.shift();
						try {
							('function(){return ' + varn.join('\',') + '}').eval()
						} catch (e) {
							break
						}
					}
				};
				meldFout(e, m)
			}
		}
	})(),
	Module = Class.subClass(),
	cwf = Module($);
	cwf.pad = $;
	var _cwfRoot, laadScript = function(url) {
		var s = document.createElement('script');
		s.src = url;
		return document.documentElement.firstChild.appendChild(s)
	};
	onload = function() {
		try {
			delete window.onload
		} catch (e) {
			window.onload = function() {}
		}
		cwfLoader = function(s, n) {
			_cwfLoader(s, n)
		};
		var d = document,
		dE = d.documentElement,
		src, scripts = d.getElementsByTagName('script'),
		scriptNaam = 'cwf.js';
		cwfLoader.w = window;
		if (dE.firstChild.tagName == 'SCRIPT') {
			dE.appendChild(d.createElement('head')).appendChild(dE.firstChild);
			dE.appendChild(d.createElement('body'))
		};
		var gevonden;
		for (var x = 0; x < scripts.length; x++) {
			src = scripts[x].getAttribute('src');
			if (!src) continue;
			var vr = src.indexOf('?'),
			args;
			if (vr > -1) {
				args = src.slice(vr + 1);
				src = src.slice(0, vr)
			} else args = '';
			if (src && src.toLowerCase().eindigtMet(scriptNaam)) {
				cwfLoader.args = args;
				cwfLoader.virtueel = scripts[x].getAttribute('virtueel') == 'true';
				scripts[x].parentNode.removeChild(scripts[x]);
				gevonden = true;
				break
			}
		};
		if (!gevonden) {
			src = 'cwf.js';
			cwfLoader.virtueel = true
		}
		_cwfRoot = src.replace(scriptNaam, '');
		cwfLoader.s = laadScript(src + '')
	};
	if (document.body) onload()
};
maakModule = function(pad) {}
