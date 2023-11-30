
project(onig)

set( ONIG_SOURCES
	    regcomp.c
        regenc.c
        regerror.c
        regexec.c
        regparse.c
        regsyntax.c
        regtrav.c
        regversion.c
        st.c
        enc/ascii.c
        enc/cp1251.c
        enc/koi8_r.c
        enc/unicode.c
        enc/utf16_le.c
        enc/utf32_le.c )

add_library( onig SHARED ${ONIG_SOURCES} )