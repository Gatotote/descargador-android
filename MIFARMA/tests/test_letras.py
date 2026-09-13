from letras import entero_en_letras, monto_en_letras


def test_ticket_real_271_pesos():
    assert monto_en_letras(271) == "==(DOSCIENTOS SETENTA Y UN PESOS 00/100 M.N.)=="


def test_otros_importes():
    assert entero_en_letras(1) == "UN"
    assert monto_en_letras(1) == "==(UN PESO 00/100 M.N.)=="
    assert monto_en_letras(21.5) == "==(VEINTIUN PESOS 50/100 M.N.)=="
    assert monto_en_letras(100) == "==(CIEN PESOS 00/100 M.N.)=="
    assert monto_en_letras(101) == "==(CIENTO UN PESOS 00/100 M.N.)=="
    assert monto_en_letras(2.5) == "==(DOS PESOS 50/100 M.N.)=="
