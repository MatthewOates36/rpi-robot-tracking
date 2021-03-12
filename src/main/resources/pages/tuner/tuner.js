const body = $(document.body)
const sliders = $('#sliders')

const createDoubleSlider = (label, min = 0, max = 100, initialLow, initialHigh, update) => {
    if (undefined === initialLow) {
        initialLow = min
    }

    if (undefined === initialHigh) {
        initialHigh = max
    }

    const wrapper = $(`
        <div class="double-slider-wrapper">
            <div class="slider-label">${label}</div>
            <input type="text" class="slider-input left-input">
            <div class="slider"></div>
            <input type="text" class="slider-input right-input">
        </div>
    `)

    const slider = wrapper.children('.slider')[0]

    noUiSlider.create(slider, {
        start: [initialLow, initialHigh],
        connect: true,
        step: 0.0001,
        range: {
            min: min,
            max: max
        }
    })

    sliders.append(wrapper)

    const leftInput = wrapper.children('.left-input')
    const rightInput = wrapper.children('.right-input')

    slider.noUiSlider.on('update', (values, handle) => {
        let value = values[handle]

        if (update) {
            update(values[0], values[1])
        }

        if (handle) {
            if (!rightInput.is(":focus")) {
                rightInput.val(value)
            }
        } else {
            if (!leftInput.is(":focus")) {
                leftInput.val(value)
            }
        }
    })

    leftInput.on('input', () => {
        slider.noUiSlider.set([Math.min(leftInput.val(), slider.noUiSlider.get()[1]), null])
    })

    rightInput.on('input', () => {
        slider.noUiSlider.set([null, Math.max(rightInput.val(), slider.noUiSlider.get()[0])])
    })

    leftInput.on('change', () => {
        slider.noUiSlider.set([Math.min(leftInput.val(), slider.noUiSlider.get()[1]), null])
        leftInput.val(slider.noUiSlider.get()[0])
    })

    rightInput.on('change', function () {
        slider.noUiSlider.set([null, Math.max(rightInput.val(), slider.noUiSlider.get()[0])])
        rightInput.val(slider.noUiSlider.get()[1])
    })
}

const createSingleSlider = (label, min = 0, max = 100, initial, update) => {
    if (undefined === initial) {
        initial = min
    }

    const wrapper = $(`
        <div class="single-slider-wrapper">
            <div class="slider-label">${label}</div>
            <input type="text" class="slider-input left-input">
            <div class="slider"></div>
        </div>
    `)

    const slider = wrapper.children('.slider')[0]

    noUiSlider.create(slider, {
        start: [initial],
        connect: 'lower',
        step: 0.0001,
        range: {
            min: min,
            max: max
        }
    })

    sliders.append(wrapper)

    const input = wrapper.children('.left-input')

    slider.noUiSlider.on('update', (values, handle) => {
        let value = values[0]

        if (update) {
            update(value)
        }

        if (!input.is(":focus")) {
            input.val(value)
        }
    })

    input.on('input', () => {
        slider.noUiSlider.set(input.val())
    })

    input.on('change', () => {
        slider.noUiSlider.set(input.val())
        input.val(slider.noUiSlider.get())
    })
}

const createValueDisplay = (formatText = "", url) => {
    const updateData = (url, elem) => {
        getData(url).then(data => {
            let labelText = formatText;
            data.forEach(datum => labelText = labelText.replace("{}", datum))
            labelText = labelText.replaceAll("{}", "")
            elem.text(labelText)
        })
    }
    const wrapper = $(`<div class="value-display">${formatText}</div>`)
    updateData(url, wrapper)
    setInterval(() => updateData(url, wrapper), 500)
    sliders.append(wrapper)
}

const createDropDown = (key, label, selections, initialSelection) => {
    if (undefined === label) {
        label = key
    }

    const wrapper = $(`
    <div class="drop-down">
        <div class="drop-down-label">${label}</div>
        <div class="selection-wrapper"><select></select>&#x25BC;</div>
    </div>`)

    const select = wrapper.children('.selection-wrapper').children('select')

    selections.forEach(selection => {
        const selectionWrapper = $(`<option value="${selection}" ${initialSelection == selection ? "selected" : ""}>${selection}</option>`)
        select.append(selectionWrapper)
    })

    select.change(e => {
        updateData[key] = select.val()
    })

    updateData[key] = initialSelection

    sliders.append(wrapper)
}

const getData = async (url = '') => {
    const response = await fetch(url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        },
    });
    return response.json();
}

const postData = async (url = '', data = {}) => {
    const response = await fetch(url, {
        method: 'POST',
        cache: 'no-cache',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });
}

let updateData = {}

setInterval(async () => {
    if (Object.keys(updateData).length > 0) {
        await postData('/api/update-setting', updateData)
        updateData = {}
    }
}, 500)

getData('/api/ui-config').then(json => {
    for (let element of json) {
        switch (element.type) {
            case 'double-slider': {
                let initialLow = undefined
                let initialHigh = undefined
                if (element.initial) {
                    initialLow = element.initial.low
                    initialHigh = element.initial.high
                }

                createDoubleSlider(element.key, element.config.min, element.config.max, initialLow, initialHigh, (low, high) => {
                    updateData[element.key + ":low"] = low
                    updateData[element.key + ":high"] = high
                })
                break
            }
            case 'single-slider': {
                let initialValue = undefined
                if (element.initial) {
                    initialValue = element.initial.value
                    console.log(initialValue)
                }

                createSingleSlider(element.key, element.config.min, element.config.max, initialValue, (value) => {
                    updateData[element.key] = value
                })
                break
            }
            case 'value-display': {
                let url = `/api/get-value/${element.key}/`
                createValueDisplay(element.config.format, url)
                break
            }
            case 'drop-down': {
                createDropDown(element.key, element.config.title, element.config.selections, element.initial.value)
                break
            }
        }
    }
})