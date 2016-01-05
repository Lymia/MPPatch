#!/usr/bin/env python2

import sys
data = filter(lambda x: len(x) > 0, open(sys.argv[1]).read().split("\n"))

print '#include "c_rt.h"'
print '#include "c_defines.h"'
print '#include "extern_defines.h"'
print ''
for line in data:
  name, attr, ret, paramStr = line.split(":")
  paramNames = ', '.join(map(lambda x: x.strip().rsplit(" ", 1)[1], paramStr.split(",")))
  print '// Proxy for '+name
  print 'typedef '+attr+' '+ret+' (*'+name+'_fn) ('+paramStr+');'
  print 'static '+name+'_fn '+name+'_ptr;'
  print ret+' '+name+'('+paramStr+') {'
  print '  return '+name+'_ptr('+paramNames+');'
  print '}'
  print '__attribute__((constructor(400))) static void '+name+'_loader() {'
  print '  '+name+'_ptr = ('+name+'_fn) resolveAddress('+name+'_offset);'
  print '}'
  print ''
